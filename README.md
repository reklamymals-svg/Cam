# Monitoring SUPLA — XMEye (wersja dostosowana)

Projekt Android Studio (Kotlin), przygotowany do konfiguracji czterech kamer współpracujących z XMEye, temperatury z konta SUPLA i dwóch przycisków do linków konfigurowanych później.

## Ważne: XMEye

**Nie ma tu natywnej integracji z chmurą P2P XMEye.** Numer seryjny/identyfikator chmurowy nie wystarczy do odtwarzania. Projekt wyświetla do 4 strumieni **RTSP**, o ile rejestrator XMEye udostępnia ten protokół. Ustal model rejestratora i obsługę RTSP/ONVIF. Nie wystawiaj RTSP bezpośrednio do Internetu; dla dostępu zdalnego użyj bezpiecznego VPN. Nie wpisuj haseł i tokenów w czacie.

## Funkcje

- Cztery odtwarzacze RTSP (układ 2 × 2), dotknięcie podglądu przełącza na pełny ekran.
- Pole modelu rejestratora XMEye i osobne adresy RTSP dla każdej kamery.
- SUPLA: żądanie HTTPS z tokenem Bearer co 60 sekund; odczyt pól `temperature` albo `value.temperature`.
- Dwa przyciski: nazwy i adresy HTTP(S) można ustawić w dowolnym momencie.

## Uruchomienie

1. Otwórz katalog `MonitoringSupla` w Android Studio z JDK 17.
2. Zsynchronizuj Gradle, następnie wybierz Build > Build APK(s).
3. Zainstaluj APK na Androidzie 8.0 lub nowszym.
4. W Ustawieniach wpisz model rejestratora i działające adresy RTSP, adres endpointu SUPLA oraz token.
5. Linki dwóch przycisków możesz uzupełnić później.

## Bezpieczeństwo i status

To **projekt źródłowy, nie gotowy APK**. Nie był kompilowany ani testowany na fizycznym telefonie. Token SUPLA jest obecnie zapisywany w zwykłych SharedPreferences — do produkcji należy wdrożyć magazyn szyfrowany i odświeżanie tokenu. Wymagana jest weryfikacja formatu odpowiedzi REST konkretnego kanału SUPLA, modelu XMEye i obsługiwanych kodeków RTSP. Nie obsługuje PTZ, nagrań, archiwum ani zamkniętej chmury XMEye.

## Techage / ONVIF — aktualizacja
W ustawieniach każdej z czterech kamer dodano adres usługi ONVIF, użytkownika, hasło i przycisk **Pobierz RTSP z ONVIF**. Klient wykonuje GetCapabilities → GetProfiles → GetStreamUri przez SOAP WS-Security UsernameToken PasswordDigest i wpisuje zwrócony RTSP do pola kamery. Po pobraniu wybierz **Zapisz i wróć**. Wymagany dostęp do sieci kamery, poprawny czas na kamerze i włączona usługa ONVIF. Implementacja dotyczy ONVIF Media1; niektóre modele mogą wymagać innej ścieżki, sposobu uwierzytelniania albo Media2. Funkcji nie przetestowano na sprzęcie Techage. Hasła i token SUPLA są w zwykłych SharedPreferences — to prototyp, nie gotowa bezpieczna wersja produkcyjna.

## Automatyczny start — sieć domowa

Po uruchomieniu aplikacja otwiera bezpośrednio pulpit i próbuje odtworzyć wszystkie cztery zapisane strumienie RTSP. Odczyt SUPLA uruchamia się od razu, następnie jest ponawiany co 60 sekund. Po powrocie z tła pulpit ponownie uruchamia strumienie. Przy braku konfiguracji aplikacja pokazuje odpowiednie komunikaty, a przy błędzie odtwarzania wskazuje brak obrazu.

Dostęp do kamer jest przewidziany wyłącznie w domowej sieci Wi-Fi (lokalne adresy IP); nie skonfigurowano dostępu przez chmurę XMEye. SUPLA nadal potrzebuje internetu. W celu pierwszego uruchomienia należy jednorazowo ustawić cztery kamery ONVIF / RTSP i dostęp SUPLA.
