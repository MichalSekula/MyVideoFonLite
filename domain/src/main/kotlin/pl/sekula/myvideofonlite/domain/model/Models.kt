package pl.sekula.myvideofonlite.domain.model

/** Status urządzenia. UNKNOWN nigdy nie jest traktowany jako online (spec. rozdz. 9). */
enum class DeviceStatus { ONLINE, OFFLINE, UNKNOWN }

/** Funkcje, które konkretne urządzenie/SDK faktycznie udostępnia. Niczego nie zakładamy z góry. */
enum class Capability { LIVE_VIDEO, TWO_WAY_AUDIO, GATE_UNLOCK, DOORBELL_EVENTS }

data class DoorbellDevice(
    val id: String,
    val name: String,
    val status: DeviceStatus,
    val capabilities: Set<Capability>,
) {
    fun supports(capability: Capability): Boolean = capability in capabilities
}

/** Krótki opis urządzenia na liście wyboru w ustawieniach. */
data class DeviceSummary(val id: String, val name: String)

enum class EventType { RING, MOTION }

data class DoorbellEvent(
    val deviceId: String,
    val type: EventType,
    val timestampMillis: Long,
)

/** Źródło danych aplikacji: tryb demo (symulator) albo oficjalne SDK Tuya. */
enum class BackendMode { DEMO, TUYA }

data class AppSettings(
    val backendMode: BackendMode = BackendMode.DEMO,
    val selectedDeviceId: String? = null,
    val requireBiometricForGate: Boolean = true,
)

/** Błędy rozróżniane w UI (spec. rozdz. 9). */
enum class DomainError {
    NOT_CONFIGURED,
    AUTH,
    CONNECTION,
    DEVICE_OFFLINE,
    UNSUPPORTED,
    NOT_CONFIRMED,
}

/** Czytelny komunikat dla użytkownika (spec. rozdz. 9 i kryterium akceptacji 10). */
fun DomainError.defaultMessage(): String = when (this) {
    DomainError.NOT_CONFIGURED -> "Nie skonfigurowano urządzenia lub integracji."
    DomainError.AUTH -> "Błąd autoryzacji. Zaloguj się ponownie."
    DomainError.CONNECTION -> "Błąd połączenia. Spróbuj ponownie."
    DomainError.DEVICE_OFFLINE -> "Urządzenie jest offline."
    DomainError.UNSUPPORTED -> "Ta funkcja nie jest obsługiwana przez urządzenie."
    DomainError.NOT_CONFIRMED -> "Ta akcja wymaga potwierdzenia."
}

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError, val message: String? = null) : Outcome<Nothing>
}

sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val accountLabel: String) : AuthState
    data object Expired : AuthState
}

sealed interface LiveViewState {
    data object Idle : LiveViewState
    data object Connecting : LiveViewState
    data class Connected(val audioEnabled: Boolean, val talking: Boolean) : LiveViewState
    data object Disconnected : LiveViewState
    data class Error(val error: DomainError, val message: String? = null) : LiveViewState
}
