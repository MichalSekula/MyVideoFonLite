# Integracja z Tuya – instrukcja (Etap 0 → Etap 2)

**Stan:** integracja podłączona (Tuya Smart Life App SDK 7.11 + IPC SDK 7.11.1).
Kod: `app/src/main/java/pl/sekula/myvideofonlite/data/tuya/`:
- `TuyaSdk` – leniwa inicjalizacja SDK (tylko w trybie Tuya i tylko z kluczami),
- `TuyaBackend` – logowanie e-mailem, domy i urządzenia, status, nasłuch DP (dzwonek, ruch), furtka,
- `TuyaLiveSession` – P2P, obraz w `ThingCameraView`, dźwięk i rozmowa,
- `DpMap` – wyszukiwanie DP po kodzie/nazwie (furtka: `accessory_lock` / „Lock Switch”, awaryjnie DP 148).

Ograniczenie: powiadomienie o dzwonku przychodzi, gdy aplikacja działa (nasłuch DP przez MQTT SDK).
Powiadomienia przy zamkniętej aplikacji wymagają push Tuya (Firebase) – osobny etap.

## Etap 0 – weryfikacja (obowiązkowa przed pisaniem kodu SDK)

- [ ] Ustal producenta i dokładny model wideodomofonu (naklejka / aplikacja Smart Life → urządzenie → informacje).
- [ ] Sprawdź, w której aplikacji jest dodany (Smart Life czy Tuya Smart) i region konta (Europa).
- [ ] Załóż konto na https://platform.tuya.com i utwórz projekt aplikacji **SmartLife App SDK**.
- [ ] Wygeneruj klucze AppKey/AppSecret dla nazwy pakietu `pl.sekula.myvideofonlite` i odcisku SHA-256 certyfikatu podpisu.
- [ ] Sprawdź warunki (ewentualne opłaty) i dostępność **IPC SDK** dla Twojego modelu.
- [ ] Odczytaj w panelu Tuya listę DP urządzenia: czy jest zdarzenie dzwonka i DP otwarcia elektrozaczepu.
- [ ] Zbuduj proof of concept: logowanie → lista urządzeń → obraz z kamery.

Kryterium przejścia dalej: działający obraz z konkretnego urządzenia oraz potwierdzone zdarzenie dzwonka / komenda furtki.

## Konfiguracja kluczy (poza repozytorium)

W pliku `local.properties` (jest w `.gitignore`):

```
tuya.appKey=TWOJ_APP_KEY
tuya.appSecret=TWOJ_APP_SECRET
```

Plik bezpieczeństwa SDK pobrany z panelu Tuya (np. `t_s.bmp`) umieść w `app/src/main/assets/` –
również jest ignorowany przez git. **Nigdy** nie umieszczaj w aplikacji `client_secret` z Cloud OpenAPI.

## Sekrety w GitHub Actions

W repozytorium: **Settings → Secrets and variables → Actions → New repository secret**:

| Nazwa | Wartość |
|---|---|
| `TUYA_APP_KEY` | AppKey z panelu Tuya |
| `TUYA_APP_SECRET` | AppSecret z panelu Tuya |
| `SIGNING_KEYSTORE_BASE64` | plik `myvideofonlite.jks` zakodowany w base64 |
| `SIGNING_STORE_PASSWORD` | hasło do keystore |
| `SIGNING_KEY_ALIAS` | `myvideofonlite` |
| `SIGNING_KEY_PASSWORD` | hasło klucza (zwykle to samo co keystore) |

Base64 pliku: Windows PowerShell `[Convert]::ToBase64String([IO.File]::ReadAllBytes("myvideofonlite.jks")) | Set-Clipboard`,
macOS/Linux `base64 -i myvideofonlite.jks | pbcopy` (lub `base64 -w0 myvideofonlite.jks`).

Lokalnie (Android Studio) te same wartości w `local.properties`:

```
signing.storeFile=C:/sciezka/do/myvideofonlite.jks
signing.storePassword=...
signing.keyAlias=myvideofonlite
signing.keyPassword=...
```

## Podłączenie SDK

1. Odkomentuj repozytorium Maven Tuya w `settings.gradle.kts`.
2. Dodaj zależności SmartLife App SDK i IPC SDK w `app/build.gradle.kts` w wersjach z aktualnej dokumentacji Tuya.
3. Zainicjalizuj SDK w `MyVideoFonApp.onCreate()` (miejsce oznaczone `TODO(TUYA)`).
4. Zaimplementuj metody `TuyaBackend`:
   - `signIn` / `signOut` – logowanie kontem Smart Life przez SDK,
   - `listDevices` – urządzenia z domu użytkownika (kategoria wideodomofon),
   - `fetchDevice` – status online i mapowanie funkcji na `Capability`
     (`LIVE_VIDEO`, `TWO_WAY_AUDIO`, `GATE_UNLOCK`, `DOORBELL_EVENTS`) – **tylko te, które urządzenie naprawdę ma**,
   - `openGate` – wysłanie DP elektrozaczepu,
   - `createLiveSession` – sesja P2P z IPC SDK; widok renderujący przychodzi przez `attachRenderTarget(FrameLayout)`,
   - `incomingEvents` – zdarzenia dzwonka (push Tuya / DP).
5. W ustawieniach aplikacji przełącz źródło danych na „Tuya”.

## Zasady (ze specyfikacji)

- Tylko oficjalne SDK / udokumentowane protokoły – bez nieudokumentowanych adresów RTSP/HTTP.
- Nie logować tokenów, haseł ani danych sesji.
- Otwieranie furtki wyłącznie po świadomym potwierdzeniu (już zaimplementowane w UI i w `OpenGate`).
