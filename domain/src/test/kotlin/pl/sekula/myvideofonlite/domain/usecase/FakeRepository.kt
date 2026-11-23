package pl.sekula.myvideofonlite.domain.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.sekula.myvideofonlite.domain.model.AuthState
import pl.sekula.myvideofonlite.domain.model.Capability
import pl.sekula.myvideofonlite.domain.model.DeviceStatus
import pl.sekula.myvideofonlite.domain.model.DeviceSummary
import pl.sekula.myvideofonlite.domain.model.DoorbellDevice
import pl.sekula.myvideofonlite.domain.model.DoorbellEvent
import pl.sekula.myvideofonlite.domain.model.DomainError
import pl.sekula.myvideofonlite.domain.model.LiveViewState
import pl.sekula.myvideofonlite.domain.model.Outcome
import pl.sekula.myvideofonlite.domain.repository.DoorbellRepository
import pl.sekula.myvideofonlite.domain.repository.LiveViewSession

fun device(
    status: DeviceStatus = DeviceStatus.ONLINE,
    caps: Set<Capability> = Capability.entries.toSet(),
) = DoorbellDevice("dev1", "Wejście", status, caps)

class FakeRepository(initial: DoorbellDevice? = device()) : DoorbellRepository {
    override val device = MutableStateFlow(initial)
    override val events = MutableStateFlow<List<DoorbellEvent>>(emptyList())
    override val authState = MutableStateFlow<AuthState>(AuthState.SignedIn("demo"))
    override val lastError = MutableStateFlow<String?>(null)
    var gateOpenedFor: String? = null
    var sessionFails = false

    override suspend fun signIn(countryCode: String, email: String, password: String) = Outcome.Success(Unit)
    override suspend fun signOut() {}
    override suspend fun listDevices() = Outcome.Success(listOf(DeviceSummary("dev1", "Wejście")))
    override suspend fun refreshStatus(): Outcome<DoorbellDevice> = Outcome.Success(device.value!!)
    override suspend fun openGate(deviceId: String): Outcome<Unit> {
        gateOpenedFor = deviceId
        return Outcome.Success(Unit)
    }
    override suspend fun recordEvent(event: DoorbellEvent) {
        events.value = listOf(event) + events.value
    }
    override fun createLiveSession(deviceId: String): LiveViewSession = FakeSession(sessionFails)
}

class FakeSession(private val fails: Boolean) : LiveViewSession {
    private val _state = MutableStateFlow<LiveViewState>(LiveViewState.Idle)
    override val state: StateFlow<LiveViewState> = _state
    override suspend fun connect() {
        _state.value = if (fails) LiveViewState.Error(DomainError.CONNECTION)
        else LiveViewState.Connected(audioEnabled = false, talking = false)
    }
    override suspend fun disconnect() { _state.value = LiveViewState.Disconnected }
    override suspend fun setAudioEnabled(enabled: Boolean) {}
    override suspend fun setTalking(talking: Boolean) {}
}
