# Mini aplikacja Android do sterowania wideodomofonem Tuya

**Wersja:** 1.0\
**Data:** 23 listopada 2026\
**Status:** Koncepcja / specyfikacja MVP

## 1. Cel projektu

Celem projektu jest stworzenie niewielkiej, własnej aplikacji na
Androida do obsługi wideodomofonu działającego w ekosystemie Tuya oraz
towarzyszącego jej widgetu na ekranie głównym telefonu.

Aplikacja ma zapewnić szybki dostęp do najważniejszych funkcji bez
konieczności otwierania Smart Life. Interfejs powinien być prosty,
czytelny i skupiony na wideodomofonie.

Podstawowe założenia:

-   własny interfejs Android, niezależny wizualnie od Smart Life;
-   podgląd obrazu z wideodomofonu;
-   obsługa rozmowy dwukierunkowej, o ile urządzenie i SDK ją
    udostępniają;
-   odbieranie informacji o naciśnięciu przycisku dzwonka;
-   szybki dostęp do funkcji otwierania furtki/bramy, jeśli urządzenie
    udostępnia taką funkcję;
-   widget z podstawowym statusem i skrótami;
-   możliwość rozbudowy o kolejne urządzenia Tuya.

**Ważne:** zgodność z Tuya nie oznacza automatycznie, że każde
urządzenie może być obsługiwane przez dowolne SDK Tuya. Przed
implementacją należy zweryfikować dokładny model wideodomofonu, jego
firmware, sposób parowania oraz dostępność funkcji w Tuya IPC SDK /
SmartLife App SDK. Nie należy zakładać, że samo Tuya Cloud OpenAPI
zapewni transmisję audio-wideo.

## 2. Zakres funkcjonalny

### 2.1. MVP --- wymagane funkcje

  ------------------------------------------------------------------------
  Obszar                Funkcja                                  Priorytet
  --------------------- --------------------- ----------------------------
  Urządzenie            Konfiguracja i                                  P0
                        powiązanie            
                        konta/urządzenia Tuya 

  Status                Wyświetlanie                                    P0
                        dostępności           
                        urządzenia            

  Wideo                 Uruchomienie podglądu                           P0
                        na żywo               

  Dzwonek               Obsługa zdarzenia                               P0
                        naciśnięcia przycisku 

  Powiadomienia         Powiadomienie Android                           P0
                        o zdarzeniu dzwonka   

  Sterowanie            Otwieranie                                      P0
                        furtki/bramy, jeżeli  
                        urządzenie udostępnia 
                        odpowiednią komendę   

  Widget                Status urządzenia i                             P0
                        skrót do              
                        aplikacji/podglądu    

  Ustawienia            Wybór urządzenia i                              P1
                        podstawowa            
                        konfiguracja          
  ------------------------------------------------------------------------

### 2.2. Poza zakresem pierwszej wersji

-   obsługa wielu producentów spoza ekosystemu Tuya;
-   własny serwer przechowujący nagrania;
-   zaawansowany system automatyzacji domu;
-   ciągły streaming wideo w samym widgecie;
-   udostępnianie urządzeń innym użytkownikom aplikacji;
-   pełne zastąpienie wszystkich funkcji Smart Life.

## 3. Główne scenariusze użytkownika

### Scenariusz A --- użytkownik otwiera aplikację

1.  Uruchamia aplikację.
2.  Aplikacja prezentuje skonfigurowany wideodomofon.
3.  Użytkownik widzi status urządzenia oraz dostępne akcje.
4.  Wybiera „Podgląd na żywo".
5.  Aplikacja inicjuje sesję P2P i wyświetla obraz, jeśli urządzenie
    jest dostępne.
6.  Użytkownik może włączyć audio i rozpocząć rozmowę, jeśli urządzenie
    wspiera tę funkcję.

### Scenariusz B --- ktoś naciska przycisk dzwonka

1.  Urządzenie generuje zdarzenie dzwonka.
2.  Aplikacja otrzymuje zdarzenie przez mechanizm dostępny dla danego
    modelu/SDK.
3.  Android wyświetla powiadomienie.
4.  Użytkownik wybiera powiadomienie.
5.  Aplikacja otwiera ekran wideodomofonu i próbuje uruchomić podgląd.
6.  Jeśli sesja wideo nie może zostać zestawiona, aplikacja wyświetla
    czytelny komunikat i pozwala ponowić próbę.

Dostarczanie zdarzeń w tle zależy od możliwości urządzenia, SDK,
uprawnień Androida, usług producenta oraz ograniczeń oszczędzania
energii. Należy to przetestować na docelowych telefonach.

### Scenariusz C --- użytkownik korzysta z widgetu

1.  Użytkownik widzi widget na ekranie głównym.
2.  Widget pokazuje ostatni znany status urządzenia.
3.  Dotknięcie widgetu otwiera odpowiedni ekran aplikacji.
4.  Opcjonalne akcje, np. otwarcie furtki, mogą być dostępne
    bezpośrednio z widgetu, ale powinny wymagać bezpiecznego
    potwierdzenia.

Widget nie powinien sugerować, że prezentuje obraz na żywo, jeśli
pokazuje jedynie ostatnią miniaturę lub statyczny podgląd.

## 4. Proponowany interfejs

### 4.1. Ekran główny

Elementy:

-   nazwa urządzenia, np. „Wideodomofon --- wejście";
-   status: online / offline / nieznany;
-   przycisk „Podgląd na żywo";
-   przycisk „Otwórz furtkę" --- tylko jeśli dostępny i poprawnie
    zidentyfikowany;
-   data i godzina ostatniego zdarzenia;
-   informacja o błędzie połączenia, jeżeli wystąpił.

### 4.2. Ekran podglądu

Elementy:

-   obraz z kamery;
-   wskaźnik łączenia / połączono / rozłączono;
-   sterowanie audio;
-   przycisk rozpoczęcia/zakończenia rozmowy;
-   przycisk otwierania furtki/bramy, jeżeli wspierany;
-   przycisk ponownego połączenia;
-   obsługa orientacji ekranu i pełnego ekranu.

Nie należy implementować własnego odtwarzania strumienia na podstawie
nieudokumentowanych adresów RTSP/HTTP ani omijać mechanizmów autoryzacji
producenta. Preferowany jest oficjalny SDK lub udokumentowany protokół
urządzenia.

### 4.3. Powiadomienie o dzwonku

Powiadomienie powinno zawierać:

-   nazwę urządzenia;
-   informację „Ktoś dzwoni do drzwi";
-   czas zdarzenia;
-   akcję „Otwórz aplikację" lub „Podgląd".

Opcjonalnie można dodać akcję otwarcia furtki, ale ze względu na
bezpieczeństwo lepiej kierować użytkownika do aplikacji i wymagać
potwierdzenia.

## 5. Widget Android

### 5.1. Cel widgetu

Widget ma być szybkim panelem informacyjnym i skrótem do najważniejszych
funkcji. Nie jest pełnym ekranem wideodomofonu.

### 5.2. Proponowany wygląd i zawartość

Widget w rozmiarze 2x2 lub 4x2:

-   nazwa urządzenia;
-   status online/offline;
-   informacja o ostatnim dzwonku;
-   przycisk „Podgląd" otwierający aplikację;
-   opcjonalny przycisk „Furtka", jeśli funkcja jest dostępna i
    bezpiecznie obsłużona;
-   opcjonalna miniatura ostatniego obrazu, z wyraźnym oznaczeniem czasu
    jej pobrania.

### 5.3. Ograniczenia techniczne

-   Standardowy Android App Widget nie jest przeznaczony do ciągłego
    odtwarzania strumienia wideo.
-   Odświeżanie widgetów podlega ograniczeniom systemu Android i nie
    powinno opierać się na bardzo częstym polling'u.
-   Widget powinien prezentować ostatni znany stan, a nie gwarantować
    stan rzeczywisty w każdej chwili.
-   Akcje wrażliwe, takie jak otwarcie furtki, powinny być zabezpieczone
    potwierdzeniem lub przejściem do aplikacji.
-   Do aktualizacji widgetu należy używać mechanizmów systemowych, np.
    WorkManager i odpowiednich callbacków/zdarzeń, z uwzględnieniem
    limitów Androida.

## 6. Architektura techniczna

### 6.1. Proponowany stos technologiczny

  Warstwa                    Technologia
  -------------------------- ------------------------------------
  Język                      Kotlin
  UI aplikacji               Jetpack Compose
  Widget                     Android AppWidget / Glance
  Architektura               MVVM lub UDF
  Asynchroniczność           Kotlin Coroutines + Flow
  Wstrzykiwanie zależności   Hilt lub Koin
  Trwałe ustawienia          DataStore
  Praca odroczona            WorkManager
  Integracja Tuya            SmartLife App SDK oraz/lub IPC SDK
  Powiadomienia              Android Notification APIs
  Testy                      JUnit, MockK, Compose UI Test

Wybór dokładnej wersji SDK Tuya należy oprzeć na aktualnej dokumentacji
i wymaganiach konta deweloperskiego.

### 6.2. Logiczny podział modułów

``` text
app/
  presentation/
    home/
    liveview/
    settings/
    events/
  widget/
  domain/
    model/
    repository/
    usecase/
  data/
    tuya/
      account/
      device/
      ipc/
      events/
    local/
  notifications/
  workers/
```

### 6.3. Odpowiedzialności warstw

**Presentation** - wyświetlanie ekranów; - obsługa interakcji
użytkownika; - prezentowanie stanów ładowania, błędów i połączenia.

**Domain** - modele urządzeń i zdarzeń; - przypadki użycia, np.
`ConnectToDoorbell`, `StartLiveView`, `OpenGate`; - interfejsy
repozytoriów niezależne od SDK.

**Data** - implementacja repozytoriów; - integracja z Tuya SDK; -
mapowanie odpowiedzi SDK na modele domenowe; - lokalny zapis
konfiguracji.

**Widget** - pobieranie ostatniego znanego stanu; - aktualizacja
widoku; - obsługa kliknięć i przekierowanie do aplikacji.

**Notifications** - kanały powiadomień; - obsługa zdarzeń dzwonka; -
nawigacja z powiadomienia do odpowiedniego ekranu.

## 7. Integracja z Tuya

### 7.1. Tuya SmartLife App SDK

Należy sprawdzić, czy SDK umożliwia:

-   logowanie lub autoryzację konta Tuya;
-   pobieranie listy urządzeń użytkownika;
-   odczyt statusu urządzenia;
-   odczyt i wysyłanie komend DP (Data Points);
-   subskrypcję zmian stanu i zdarzeń.

Dokładny sposób logowania, region, konfiguracja projektu i wymagane
klucze zależą od aktualnego modelu integracji Tuya.

### 7.2. Tuya IPC SDK

Dla wideodomofonu należy zweryfikować dostępność:

-   inicjalizacji i autoryzacji sesji;
-   połączenia P2P;
-   podglądu live video;
-   dwukierunkowego audio;
-   sterowania jakością obrazu;
-   obsługi rozłączenia i ponownego połączenia;
-   zdarzeń dzwonka;
-   ewentualnego odtwarzania nagrań.

Nie należy zakładać, że wszystkie te funkcje są dostępne dla każdego
modelu urządzenia.

### 7.3. Tuya Cloud OpenAPI

Cloud OpenAPI może być przydatne do odczytu stanu i sterowania
urządzeniami, ale nie należy traktować go jako zamiennika IPC SDK dla
transmisji audio-wideo.

Jeśli aplikacja ma korzystać z własnego backendu, backend może
pośredniczyć w operacjach chmurowych. Sekrety i klucze dostępu nie
powinny być umieszczane w aplikacji Android.

### 7.4. Weryfikacja kompatybilności --- etap obowiązkowy

Przed rozpoczęciem właściwej implementacji należy:

1.  Ustalić dokładną nazwę producenta i model wideodomofonu.
2.  Sprawdzić, czy urządzenie jest dodane do Smart Life/Tuya i na jakim
    koncie/regionie.
3.  Odczytać identyfikator produktu i dostępne funkcje urządzenia, o ile
    są dostępne w panelu Tuya.
4.  Zweryfikować, czy model jest obsługiwany przez IPC SDK.
5.  Potwierdzić, czy można uzyskać live view i audio z aplikacji innej
    niż Smart Life.
6.  Sprawdzić dostępność zdarzenia dzwonka i komendy otwierania furtki.
7.  Zbudować mały proof of concept przed projektowaniem pełnego UI.

**Kryterium przejścia do implementacji MVP:** działający test połączenia
z konkretnym urządzeniem, uruchomienie obrazu oraz potwierdzenie obsługi
wymaganych zdarzeń i komend.

## 8. Bezpieczeństwo

Wideodomofon i otwieranie wejścia wymagają szczególnej ochrony.

-   Nie przechowywać `client_secret`, kluczy API ani innych sekretów
    Tuya w kodzie aplikacji.
-   Korzystać z oficjalnego procesu autoryzacji i mechanizmów
    bezpieczeństwa SDK.
-   Przechowywać tokeny zgodnie z zaleceniami Tuya i Android Keystore,
    jeśli dany mechanizm na to pozwala.
-   Nie logować tokenów, haseł, danych sesji ani wrażliwych danych
    urządzenia.
-   Otwieranie furtki powinno wymagać świadomej akcji użytkownika.
-   Rozważyć potwierdzenie otwarcia, blokadę przypadkowego kliknięcia i
    opcjonalne uwierzytelnienie biometryczne.
-   Nie udostępniać akcji otwierania furtki na ekranie blokady.
-   Ograniczyć treść powiadomień na ekranie blokady, jeśli może ujawniać
    prywatne informacje.
-   Obsługiwać wygaśnięcie sesji, błędy autoryzacji i utratę połączenia.

## 9. Obsługa błędów i stanów

Aplikacja powinna rozróżniać co najmniej następujące stany:

  Stan                            Zachowanie UI
  ------------------------------- ------------------------------------------------------
  Nie skonfigurowano urządzenia   Instrukcja konfiguracji
  Urządzenie online               Dostępny przycisk podglądu
  Urządzenie offline              Informacja o braku połączenia
  Łączenie P2P                    Wskaźnik postępu
  Połączono                       Obraz i dostępne kontrolki
  Błąd autoryzacji                Komunikat i możliwość ponownego logowania
  Błąd połączenia                 Możliwość ponowienia
  Funkcja nieobsługiwana          Ukrycie lub wyłączenie odpowiedniej akcji
  Nieznany stan                   Brak fałszywego założenia, że urządzenie jest online

## 10. Plan implementacji

### Etap 0 --- rozpoznanie urządzenia i SDK

-   [ ] Ustalenie dokładnego modelu wideodomofonu.
-   [ ] Weryfikacja kompatybilności z Tuya IPC SDK.
-   [ ] Weryfikacja dostępu do SmartLife App SDK.
-   [ ] Sprawdzenie zdarzeń dzwonka.
-   [ ] Sprawdzenie komendy otwierania furtki.
-   [ ] Przygotowanie konta/projektu w Tuya Developer Platform.
-   [ ] Przygotowanie minimalnego proof of concept.

**Rezultat:** potwierdzona ścieżka techniczna dla konkretnego
urządzenia.

### Etap 1 --- szkielet aplikacji

-   [ ] Utworzenie projektu Android w Kotlinie.
-   [ ] Konfiguracja Jetpack Compose.
-   [ ] Wdrożenie architektury MVVM/UDF.
-   [ ] Dodanie obsługi konfiguracji i logowania Tuya.
-   [ ] Przygotowanie warstwy repozytoriów i modeli domenowych.
-   [ ] Dodanie ekranu ustawień urządzenia.

**Rezultat:** aplikacja potrafi się uruchomić i uzyskać dostęp do
skonfigurowanego urządzenia.

### Etap 2 --- funkcje wideodomofonu

-   [ ] Połączenie z urządzeniem.
-   [ ] Uruchomienie podglądu na żywo.
-   [ ] Obsługa zamykania sesji.
-   [ ] Obsługa błędów i ponownego połączenia.
-   [ ] Dodanie audio i rozmowy dwukierunkowej, jeśli wspierane.
-   [ ] Dodanie sterowania furtką, jeśli wspierane.

**Rezultat:** podstawowa obsługa wideodomofonu z poziomu własnej
aplikacji.

### Etap 3 --- zdarzenia i powiadomienia

-   [ ] Odbiór zdarzenia naciśnięcia dzwonka.
-   [ ] Utworzenie kanału powiadomień.
-   [ ] Obsługa uprawnień powiadomień Androida.
-   [ ] Nawigacja z powiadomienia do ekranu podglądu.
-   [ ] Testy działania w tle i po ponownym uruchomieniu telefonu.

**Rezultat:** użytkownik otrzymuje powiadomienie o dzwonku i może
przejść do podglądu.

### Etap 4 --- widget

-   [ ] Utworzenie widgetu Android.
-   [ ] Wyświetlanie nazwy i statusu urządzenia.
-   [ ] Wyświetlanie czasu ostatniego zdarzenia.
-   [ ] Dodanie skrótu do podglądu.
-   [ ] Opcjonalne dodanie miniatury.
-   [ ] Dodanie bezpiecznej obsługi akcji furtki, jeśli ma być dostępna.
-   [ ] Aktualizacja widgetu po zdarzeniach i w ramach ograniczeń
    Androida.

**Rezultat:** widget zapewnia szybki dostęp do wideodomofonu.

### Etap 5 --- stabilizacja i publikacja

-   [ ] Testy na docelowych modelach telefonów.
-   [ ] Testy po utracie i odzyskaniu internetu.
-   [ ] Testy wygasania autoryzacji.
-   [ ] Testy zachowania aplikacji w tle.
-   [ ] Testy uprawnień i bezpieczeństwa.
-   [ ] Przygotowanie wersji instalacyjnej APK/AAB.
-   [ ] Dokumentacja konfiguracji i instalacji.

**Rezultat:** stabilna wersja MVP gotowa do prywatnego użytkowania.

## 11. Backlog --- przykładowe zadania

  ID         Zadanie                                 Priorytet
  ---------- ------------------------------------- -----------
  AND-001    Utworzyć projekt Kotlin/Compose                P0
  TUYA-001   Skonfigurować projekt Tuya i SDK               P0
  TUYA-002   Zaimplementować autoryzację                    P0
  TUYA-003   Pobrać i wybrać urządzenie                     P0
  IPC-001    Zweryfikować połączenie P2P                    P0
  IPC-002    Wyświetlić live video                          P0
  IPC-003    Obsłużyć audio                                 P1
  IPC-004    Obsłużyć zdarzenie dzwonka                     P0
  IPC-005    Zaimplementować otwieranie furtki            P0\*
  NOT-001    Dodać powiadomienia Android                    P0
  WID-001    Utworzyć widget                                P0
  WID-002    Dodać status urządzenia                        P0
  WID-003    Dodać skrót do podglądu                        P0
  SEC-001    Zabezpieczyć dane uwierzytelniające            P0
  QA-001     Przetestować offline/reconnect                 P0

`P0*` --- tylko jeśli urządzenie udostępnia funkcję otwierania i
użytkownik chce z niej korzystać.

## 12. Kryteria akceptacji MVP

MVP można uznać za ukończone, jeśli:

1.  Aplikacja potrafi połączyć się z właściwym kontem/urządzeniem Tuya.
2.  Użytkownik widzi poprawny status urządzenia.
3.  Użytkownik może uruchomić podgląd na żywo.
4.  Aplikacja poprawnie obsługuje zakończenie i ponowienie sesji.
5.  Zdarzenie dzwonka wywołuje powiadomienie, o ile mechanizm
    urządzenia/SDK to wspiera.
6.  Powiadomienie otwiera właściwy ekran aplikacji.
7.  Widget pokazuje ostatni znany status i prowadzi do podglądu.
8.  Akcja otwierania furtki --- jeśli dostępna --- działa wyłącznie po
    świadomym działaniu użytkownika.
9.  Aplikacja nie ujawnia sekretów Tuya w logach ani w publicznym
    kodzie.
10. Błędy połączenia i brak obsługi funkcji są jasno komunikowane.

## 13. Ryzyka i otwarte kwestie

  -----------------------------------------------------------------------
  Ryzyko / pytanie        Wpływ                   Sposób weryfikacji
  ----------------------- ----------------------- -----------------------
  Model wideodomofonu     Krytyczny               Test SDK na urządzeniu
  nieobsługiwany przez                            
  IPC SDK                                         

  Brak dostępu do API/SDK Wysoki                  Weryfikacja w Tuya
  dla danego produktu                             Developer Platform

  Zdarzenie dzwonka       Wysoki                  Test na docelowym
  niedostępne w tle                               telefonie

  Ograniczenia Androida   Średni/wysoki           Testy Doze,
  dla pracy w tle                                 oszczędzania baterii i
                                                  uprawnień

  Brak komendy otwierania Wysoki dla tej funkcji  Odczyt funkcji/DP
  furtki                                          urządzenia

  Zmiany wymagań SDK lub  Średni                  Sprawdzenie aktualnych
  opłat Tuya                                      warunków i dokumentacji
  -----------------------------------------------------------------------

## 14. Dokumentacja i materiały

Przed implementacją należy korzystać z aktualnej dokumentacji
producenta:

-   Tuya Developer Platform: https://developer.tuya.com/
-   Tuya SmartLife App SDK:
    https://developer.tuya.com/en/docs/app-development/
-   Tuya IPC SDK: https://developer.tuya.com/en/docs/app-development/
-   Android App Widgets:
    https://developer.android.com/develop/ui/views/appwidgets
-   Jetpack Glance:
    https://developer.android.com/develop/ui/compose/glance

Adresy i nazwy sekcji dokumentacji Tuya mogą się zmieniać. Należy
wyszukać aktualne instrukcje dla konkretnej wersji SDK i regionu konta.

## 15. Decyzje do podjęcia przed rozpoczęciem implementacji

Najważniejsze informacje, które trzeba uzupełnić:

1.  Dokładny producent i model wideodomofonu.
2.  Czy urządzenie jest dodane do aplikacji Smart Life czy Tuya Smart.
3.  Czy wymagany jest podgląd na żywo, rozmowa dwukierunkowa, czy obie
    funkcje.
4.  Czy wideodomofon steruje elektrozaczepem/furtką i czy ta funkcja
    jest dostępna w aplikacji.
5.  Minimalna wersja Androida i modele telefonów docelowych.
6.  Czy aplikacja ma być prywatna, czy docelowo dostępna dla innych
    użytkowników.

## 16. Rekomendowana kolejność prac

Nie należy zaczynać od projektowania widgetu ani pełnego UI. Najpierw
trzeba udowodnić, że konkretny wideodomofon da się obsłużyć z własnej
aplikacji.

Rekomendowana kolejność:

1.  Identyfikacja urządzenia.
2.  Proof of concept Tuya IPC SDK: połączenie i obraz.
3.  Weryfikacja audio, dzwonka i sterowania furtką.
4.  Implementacja podstawowej aplikacji.
5.  Powiadomienia.
6.  Widget.
7.  Testy bezpieczeństwa i stabilności.

**Najważniejsza zasada projektu:** aplikacja i widget mają być cienką,
wygodną warstwą użytkownika nad oficjalnymi mechanizmami Tuya. Nie
należy opierać rozwiązania na nieudokumentowanych endpointach ani
zakładać funkcji, których konkretne urządzenie nie udostępnia.
