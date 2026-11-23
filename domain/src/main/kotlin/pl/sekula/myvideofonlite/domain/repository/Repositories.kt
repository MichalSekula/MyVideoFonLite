package pl.sekula.myvideofonlite.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pl.sekula.myvideofonlite.domain.model.AppSettings
import pl.sekula.myvideofonlite.domain.model.AuthState
import pl.sekula.myvideofonlite.domain.model.BackendMode
import pl.sekula.myvideofonlite.domain.model.DeviceSummary
import pl.sekula.myvideofonlite.domain.model.DoorbellDevice
import pl.sekula.myvideofonlite.domain.model.DoorbellEvent
import pl.sekula.myvideofonlite.domain.model.LiveViewState
import pl.sekula.myvideofonlite.domain.model.Outcome

/** Interfejsy niezależne od SDK (spec. 6.3 – Domain). */
interface DoorbellRepository {
    /** Aktualnie wybrane urządzenie, null gdy nie skonfigurowano. */
    val device: Flow<DoorbellDevice?>

    /** Historia zdarzeń (najnowsze pierwsze). */
    val events: Flow<List<DoorbellEvent>>

    val authState: Flow<AuthState>

    /** Ostatni znany błąd połączenia/odświeżania, null gdy brak. */
    val lastError: Flow<String?>

    suspend fun signIn(countryCode: String, email: String, password: String): Outcome<Unit>
    suspend fun signOut()
    suspend fun listDevices(): Outcome<List<DeviceSummary>>
    suspend fun refreshStatus(): Outcome<DoorbellDevice>
    suspend fun openGate(deviceId: String): Outcome<Unit>
    suspend fun recordEvent(event: DoorbellEvent)
    fun createLiveSession(deviceId: String): LiveViewSession
}

/** Sesja podglądu na żywo (P2P). Renderowanie obrazu zapewnia warstwa prezentacji. */
interface LiveViewSession {
    val state: StateFlow<LiveViewState>
    suspend fun connect()
    suspend fun disconnect()
    suspend fun setAudioEnabled(enabled: Boolean)
    suspend fun setTalking(talking: Boolean)

    /** Podpięcie widoku renderującego (np. widok z IPC SDK). Typ zależy od implementacji. */
    fun attachRenderTarget(target: Any?) {}
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setBackendMode(mode: BackendMode)
    suspend fun setSelectedDevice(deviceId: String?)
    suspend fun setRequireBiometricForGate(required: Boolean)
}
