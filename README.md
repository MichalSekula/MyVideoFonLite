# MyVideoFon Lite

Mała aplikacja Android do obsługi wideodomofonu z ekosystemu Tuya, z widgetem na ekranie głównym.
Specyfikacja: [docs/specyfikacja.md](docs/specyfikacja.md).

## Co jest gotowe (MVP – szkielet + tryb demo)

| Obszar | Stan |
|---|---|
| Ekran główny: nazwa, status online/offline/nieznany, ostatni dzwonek, błędy | ✅ |
| Podgląd na żywo: stan łączenia, audio, rozmowa, furtka, ponowne połączenie, pełny ekran | ✅ (demo), Tuya – do podłączenia |
| Powiadomienie o dzwonku → otwiera podgląd (ograniczona treść na ekranie blokady) | ✅ |
| Otwieranie furtki: okno potwierdzenia + opcjonalnie biometria/PIN, brak akcji w powiadomieniu | ✅ |
| Widget 2x2 / 4x2: ostatni znany status, ostatni dzwonek, „Podgląd”, „Furtka” (z potwierdzeniem w aplikacji) | ✅ |
| Okresowe odświeżanie statusu (WorkManager, co 15 min) | ✅ |
| Ustawienia: źródło danych (Demo/Tuya), wybór urządzenia, biometria, narzędzia demo | ✅ |
| Historia zdarzeń | ✅ |
| Integracja Tuya SmartLife App SDK + IPC SDK | ⏳ po Etapie 0 – [docs/TUYA_INTEGRATION.md](docs/TUYA_INTEGRATION.md) |

**Tryb demo** symuluje wideodomofon, więc cały interfejs da się przetestować bez sprzętu:
w Ustawieniach wybierz „Demo”, wybierz urządzenie, użyj „Symuluj dzwonek” albo „Symuluj brak połączenia”.

## Czego potrzebujesz do testowania

**Tryb demo (bez prawdziwego domofonu):**
1. [Android Studio](https://developer.android.com/studio) (najnowsze stabilne; zawiera JDK 17 i Android SDK).
2. Telefon z Androidem 8.0+ z włączonym debugowaniem USB albo emulator z Android Studio.
3. Alternatywnie bez Android Studio: gotowy APK z zakładki **Actions** w GitHubie (artefakt `MyVideoFonLite-debug-apk`) – zainstaluj na telefonie (zgoda na instalację z nieznanych źródeł).

**Prawdziwy wideodomofon:**
4. Wideodomofon Tuya dodany do Smart Life / Tuya Smart + znany producent i model.
5. Konto deweloperskie na [platform.tuya.com](https://platform.tuya.com) z projektem SmartLife App SDK (AppKey/AppSecret).
6. Dostęp do Tuya IPC SDK dla tego modelu (sprawdzić wsparcie i ewentualne opłaty).

Własny serwer nie jest potrzebny.

## Uruchomienie

```bash
git clone https://github.com/MichalSekula/MyVideoFonLite.git
```

Otwórz folder w Android Studio → poczekaj na synchronizację Gradle → **Run ▶**.
Z linii poleceń: `./gradlew :app:assembleDebug` (APK w `app/build/outputs/apk/debug/`).
Testy: `./gradlew :domain:test :app:testDebugUnitTest`.

## Architektura

Kotlin, Jetpack Compose, MVVM/UDF, Coroutines + Flow, Koin, DataStore, WorkManager, Glance (widget).

```
domain/                      czysty moduł Kotlin (bez Androida) – modele, interfejsy, przypadki użycia
  model/  repository/  usecase/   (ConnectToDoorbell, StartLiveView, OpenGate, HandleDoorbellEvent, ...)
app/
  presentation/  home/ liveview/ settings/ events/ common/ theme/
  widget/        Glance AppWidget
  data/          DoorbellRepositoryImpl, local/ (DataStore), demo/ (symulator), tuya/ (punkt integracji SDK)
  notifications/ kanał + powiadomienie o dzwonku
  events/        odbiór zdarzeń → powiadomienie + widget
  workers/       okresowe odświeżanie statusu
```

## Bezpieczeństwo

- Klucze Tuya SDK tylko w `local.properties` (poza repozytorium); `client_secret` Cloud OpenAPI nigdy w aplikacji.
- Hasło nie jest zapisywane ani logowane; sesją zarządza SDK.
- Furtka: tylko świadoma akcja (potwierdzenie z opóźnionym przyciskiem, opcjonalnie biometria), brak akcji na ekranie blokady i w powiadomieniu.
- `allowBackup=false`.
