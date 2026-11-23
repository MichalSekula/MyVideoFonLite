package pl.sekula.myvideofonlite.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.sekula.myvideofonlite.data.demo.DemoBackend
import pl.sekula.myvideofonlite.data.local.LocalStore
import pl.sekula.myvideofonlite.data.tuya.TuyaBackend
import pl.sekula.myvideofonlite.domain.model.AuthState
import pl.sekula.myvideofonlite.domain.model.BackendMode
import pl.sekula.myvideofonlite.domain.model.DeviceStatus
import pl.sekula.myvideofonlite.domain.model.DeviceSummary
import pl.sekula.myvideofonlite.domain.model.DoorbellDevice
import pl.sekula.myvideofonlite.domain.model.DoorbellEvent
import pl.sekula.myvideofonlite.domain.model.DomainError
import pl.sekula.myvideofonlite.domain.model.Outcome
import pl.sekula.myvideofonlite.domain.model.defaultMessage
import pl.sekula.myvideofonlite.domain.repository.DoorbellRepository
import pl.sekula.myvideofonlite.domain.repository.LiveViewSession

@OptIn(ExperimentalCoroutinesApi::class)
class DoorbellRepositoryImpl(
    private val store: LocalStore,
    private val demo: DemoBackend,
    private val tuya: TuyaBackend,
    appScope: CoroutineScope,
) : DoorbellRepository {

    private val mode: Flow<BackendMode> = store.settings.map { it.backendMode }.distinctUntilChanged()
    private val currentMode: StateFlow<BackendMode> = mode.stateIn(appScope, SharingStarted.Eagerly, BackendMode.DEMO)

    private fun backendFor(mode: BackendMode): DoorbellBackend = when (mode) {
        BackendMode.DEMO -> demo
        BackendMode.TUYA -> tuya
    }

    private suspend fun backend(): DoorbellBackend = backendFor(mode.first())

    override val device: Flow<DoorbellDevice?> = combine(store.settings, store.snapshot) { settings, snap ->
        snap?.takeIf { settings.selectedDeviceId != null && it.id == settings.selectedDeviceId }
    }.distinctUntilChanged()

    override val events: Flow<List<DoorbellEvent>> = store.events

    override val authState: Flow<AuthState> = mode.flatMapLatest { backendFor(it).authState }

    override val lastError: Flow<String?> = store.lastError

    /** Zdarzenia z aktywnego źródła danych (dzwonek). */
    val incomingEvents: Flow<DoorbellEvent> = mode.flatMapLatest { backendFor(it).incomingEvents }

    override suspend fun signIn(countryCode: String, email: String, password: String): Outcome<Unit> =
        backend().signIn(countryCode, email, password)

    override suspend fun signOut() {
        backend().signOut()
        store.setSelectedDevice(null)
        store.clearSnapshot()
    }

    override suspend fun listDevices(): Outcome<List<DeviceSummary>> = backend().listDevices()

    override suspend fun refreshStatus(): Outcome<DoorbellDevice> {
        val id = store.settings.first().selectedDeviceId
            ?: return Outcome.Failure(DomainError.NOT_CONFIGURED)
        return when (val result = backend().fetchDevice(id)) {
            is Outcome.Success -> {
                store.saveSnapshot(result.value)
                store.setLastError(null)
                result
            }
            is Outcome.Failure -> {
                // Nie zakładamy, że urządzenie jest online, jeśli odświeżenie się nie udało.
                store.snapshot.first()?.takeIf { it.id == id }?.let { previous ->
                    val status = if (result.error == DomainError.DEVICE_OFFLINE) DeviceStatus.OFFLINE else DeviceStatus.UNKNOWN
                    store.saveSnapshot(previous.copy(status = status))
                }
                store.setLastError(result.message ?: result.error.defaultMessage())
                result
            }
        }
    }

    override suspend fun openGate(deviceId: String): Outcome<Unit> = backend().openGate(deviceId)

    override suspend fun recordEvent(event: DoorbellEvent) = store.addEvent(event)

    override fun createLiveSession(deviceId: String): LiveViewSession =
        backendFor(currentMode.value).createLiveSession(deviceId)
}
