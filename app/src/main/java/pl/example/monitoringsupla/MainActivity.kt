package pl.example.monitoringsupla

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import android.view.View
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : Activity() {
 private val prefs by lazy { getSharedPreferences("cam",MODE_PRIVATE) }
 private val players = mutableListOf<ExoPlayer>()
 private val handler = Handler(Looper.getMainLooper())
 private val client = OkHttpClient()
 private var running = false
 private var showingDashboard = false
 private var lastWifi = false
 private val wifiMonitor = object:Runnable {
  override fun run() {
   val connected=onWifi()
   if(connected!=lastWifi) {
    lastWifi=connected
    if(!connected) {
     players.forEach { it.stop();it.release() }
     players.clear()
    }
    if(showingDashboard) dashboard() else if(!connected) dashboard()
   }
   handler.postDelayed(this,3000)
  }
 }
 private fun onWifi():Boolean {
  val cm=getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
  val network=cm.activeNetwork ?: return false
  return cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)==true
 }
 private fun attachStream(view:PlayerView, url:String, caption:TextView?=null) {
  val player=ExoPlayer.Builder(this).build()
  view.player=player;players.add(player)
  player.addListener(object:Player.Listener {
   override fun onPlayerError(error:PlaybackException) {
    caption?.text="Błąd strumienia — ponawiam"
    handler.postDelayed({ if(players.contains(player)&&onWifi()) { player.prepare();player.playWhenReady=true } },5000)
   }
  })
  player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.playWhenReady=true
 }
 private var temperature: TextView? = null
 private val refresh = object:Runnable { override fun run() { if(running) { loadTemperature(); handler.postDelayed(this,60000) } } }
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); lastWifi=onWifi();dashboard();handler.postDelayed(wifiMonitor,3000) }
 private fun label(s:String, size:Float=17f):TextView = TextView(this).apply { text=s; textSize=size; setTextColor(Color.WHITE); setPadding(10,12,10,12) }
 private fun root():LinearLayout = LinearLayout(this).apply { orientation=1; setPadding(12,12,12,12); setBackgroundColor(Color.rgb(17,24,34)) }
 private fun button(s:String,action:()->Unit)=Button(this).apply { text=s; setOnClickListener{action()} }
 private fun stop() { running=false; handler.removeCallbacks(refresh); players.forEach{it.release()}; players.clear() }
 private fun dashboard() {
  stop(); running=true; showingDashboard=true
  val page=root()
  page.addView(label("MONITORING TECHAGE + SUPLA",22f))
  temperature=label("Temperatura: —",21f).also{page.addView(it)}
  val wifi=onWifi()
  if(!wifi) page.addView(label("Kamery działają wyłącznie przez Wi-Fi. Połącz z domową siecią."))
  val grid=GridLayout(this).apply{columnCount=2;rowCount=2}
  for(i in 1..4) {
   val box=LinearLayout(this).apply {orientation=1;setBackgroundColor(Color.BLACK)}
   val caption=label("Kamera $i",14f);box.addView(caption)
   val playerView=PlayerView(this).apply{useController=false}
   box.addView(playerView,LinearLayout.LayoutParams(-1,0,1f))
   val p=GridLayout.LayoutParams(GridLayout.spec((i-1)/2,1f),GridLayout.spec((i-1)%2,1f)).apply{width=0;height=0;setMargins(3,3,3,3)}
   grid.addView(box,p)
   val url=prefs.getString("cam$i","").orEmpty()
   if(wifi && url.startsWith("rtsp://")) {
    try {
     attachStream(playerView,url,caption)
     playerView.setOnClickListener{fullscreen(url)}
    } catch(e:Exception){caption.text="Kamera $i: błąd"}
   } else caption.text=if(!wifi) "Kamera $i: wymagane Wi-Fi" else "Kamera $i: skonfiguruj RTSP"
  }
  page.addView(grid,LinearLayout.LayoutParams(-1,0,1f))
  for(i in 1..2) {
   val title=prefs.getString("title$i","Przycisk $i")!!
   page.addView(button(title){
    val url=prefs.getString("link$i","").orEmpty()
    if(url.startsWith("https://")||url.startsWith("http://")) startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))
    else Toast.makeText(this,"Ustaw link w konfiguracji",Toast.LENGTH_SHORT).show()
   })
  }
  page.addView(button("Ustawienia"){settings()})
  setContentView(page)
  handler.post(refresh)
 }
 private fun fullscreen(url:String) {
  stop(); showingDashboard=false
  if(!onWifi()){dashboard();return}
  val page=root()
  val view=PlayerView(this)
  page.addView(view,LinearLayout.LayoutParams(-1,0,1f))
  attachStream(view,url)
  page.addView(button("Wróć do 4 kamer"){dashboard()});setContentView(page)
 }
 private fun loadTemperature(){
  val endpoint=prefs.getString("suplaUrl","").orEmpty()
  val token=prefs.getString("suplaToken","").orEmpty()
  if(!endpoint.startsWith("https://")||token.isBlank()) {temperature?.text="Temperatura SUPLA: skonfiguruj API";return}
  val req=Request.Builder().url(endpoint).header("Authorization","Bearer $token").build()
  client.newCall(req).enqueue(object:Callback {
   override fun onFailure(call:Call,e:IOException){runOnUiThread{if(running)temperature?.text="SUPLA: brak połączenia"}}
   override fun onResponse(call:Call,response:Response) {
    response.use{
     val data=it.body?.string().orEmpty()
     val result=try{
      val json=JSONObject(data)
      val value=json.optJSONObject("state")?.opt("temperature") ?: json.opt("temperature")
      if(it.isSuccessful&&value!=null) "Temperatura: $value °C" else "SUPLA: sprawdź adres API i token"
     }catch(e:Exception){"SUPLA: błąd danych"}
     runOnUiThread{if(running)temperature?.text=result}
    }
   }
  })
 }
 private fun settings() {
  stop(); showingDashboard=false
  val page=root()
  val scroll=ScrollView(this)
  val fields=LinearLayout(this).apply{orientation=1}
  val edits=mutableMapOf<String,EditText>()
  fun field(key:String,title:String) {
   fields.addView(label(title,15f))
   val edit=EditText(this).apply{
    setText(prefs.getString(key,""));setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY)
    if(key=="suplaToken") inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
   }
   edits[key]=edit;fields.addView(edit)
  }
  fields.addView(label("USTAWIENIA — adresy RTSP z kamer ONVIF",20f))
  for(i in 1..4) field("cam$i","Kamera $i — rtsp://adres:port/ścieżka")
  field("suplaUrl","SUPLA — pełny adres HTTPS endpointu API czujnika")
  field("suplaToken","SUPLA — token OAuth")
  for(i in 1..2){field("title$i","Nazwa przycisku $i");field("link$i","Link przycisku $i")}
  fields.addView(button("Zapisz i uruchom monitoring"){
   val editor=prefs.edit()
   edits.forEach{(k,v)->editor.putString(k,v.text.toString().trim())};editor.apply();dashboard()
  })
  scroll.addView(fields);page.addView(scroll);setContentView(page)
 }
 override fun onDestroy(){handler.removeCallbacks(wifiMonitor);stop();client.dispatcher.cancelAll();super.onDestroy()}
 override fun onBackPressed(){dashboard()}
}
