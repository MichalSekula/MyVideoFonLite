package pl.sekula.myvideofonlite.data.demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.sekula.myvideofonlite.data.DoorbellBackend
import pl.sekula.myvideofonlite.domain.model.AuthState
import pl.sekula.myvideofonlite.domain.model.Capability
import pl.sekula.myvideofonlite.domain.model.DeviceStatus
import pl.sekula.myvideofonlite.domain.model.DeviceSummary
import pl.sekula.myvideofonlite.domain.model.DoorbellDevice
import pl.sekula.myvideofonlite.domain.model.DoorbellEvent
import pl.sekula.myvideofonlite.domain.model.DomainError
import pl.sekula.myvideofonlite.domain.model.EventType
import pl.sekula.myvideofonlite.domain.model.LiveViewState
import pl.sekula.myvideofonlite.domain.model.Outcome
import pl.sekula.myvideofonlite.domain.repository.LiveViewSession

/**
 * Symulator wideodomofonu. Pozwala przetestować cały interfejs (ekrany, powiadomienia,
 * widget, potwierdzenie furtki) zanim zostanie zweryfikowane prawdziwe urządzenie Tuya.
 */
class DemoBackend : DoorbellBackend {

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedIn("Tryb demo"))
    override val authState: StateFlow<AuthState> = _authState

    private val _events = MutableSharedFlow<DoorbellEvent>(extraBufferCapacity = 8)
    override val incomingEvents: Flow<DoorbellEvent> = _events

    /** Przełącznik w ustawieniach: symuluje utratę połączenia z urządzeniem. */
    val simulateOffline = MutableStateFlow(false)

    override suspend fun signIn(countryCode: String, email: String, password: String): Outcome<Unit> {
        _authState.value = AuthState.SignedIn("Tryb demo")
        return Outcome.Success(Unit)
    }

    override suspend fun signOut() = Unit

    override suspend fun listDevices(): Outcome<List<DeviceSummary>> =
        Outcome.Success(listOf(DeviceSummary(DEVICE_ID, DEVICE_NAME)))

    override suspend fun fetchDevice(deviceId: String): Outcome<DoorbellDevice> {
        delay(300)
        if (deviceId != DEVICE_ID) return Outcome.Failure(DomainError.NOT_CONFIGURED, "Nieznane urządzenie demo.")
        return Outcome.Success(
            DoorbellDevice(
                id = DEVICE_ID,
                name = DEVICE_NAME,
                status = if (simulateOffline.value) DeviceStatus.OFFLINE else DeviceStatus.ONLINE,
                capabilities = Capability.entries.toSet(),
            ),
        )
    }

    override suspend fun openGate(deviceId: String): Outcome<Unit> {
        delay(500)
        return if (simulateOffline.value) Outcome.Failure(DomainError.DEVICE_OFFLINE) else Outcome.Success(Unit)
    }

    override fun createLiveSession(deviceId: String): LiveViewSession = DemoLiveSession { simulateOffline.value }

    fun simulateRing() {
        _events.tryEmit(DoorbellEvent(DEVICE_ID, EventType.RING, System.currentTimeMillis()))
    }

    companion object {
        const val DEVICE_ID = "demo-doorbell"
        const val DEVICE_NAME = "Wideodomofon – wejście (demo)"
    }
}

private class DemoLiveSession(private val isOffline: () -> Boolean) : LiveViewSession {
    private val _state = MutableStateFlow<LiveViewState>(LiveViewState.Idle)
    override val state: StateFlow<LiveViewState> = _state

    override suspend fun connect() {
        _state.value = LiveViewState.Connecting
        delay(1200)
        _state.value = if (isOffline()) {
            LiveViewState.Error(DomainError.DEVICE_OFFLINE)
        } else {
            LiveViewState.Connected(audioEnabled = false, talking = false)
        }
    }

    override suspend fun disconnect() {
        _state.value = LiveViewState.Disconnected
    }

    override suspend fun setAudioEnabled(enabled: Boolean) {
        val s = _state.value
        if (s is LiveViewState.Connected) _state.value = s.copy(audioEnabled = enabled, talking = s.talking && enabled)
    }

    override suspend fun setTalking(talking: Boolean) {
        val s = _state.value
        if (s is LiveViewState.Connected) _state.value = s.copy(talking = talking, audioEnabled = s.audioEnabled || talking)
    }
}
