package pl.sekula.myvideofonlite.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import pl.sekula.myvideofonlite.domain.model.AuthState
import pl.sekula.myvideofonlite.domain.model.Capability
import pl.sekula.myvideofonlite.domain.model.DeviceStatus
import pl.sekula.myvideofonlite.domain.model.DoorbellDevice
import pl.sekula.myvideofonlite.domain.model.DoorbellEvent
import pl.sekula.myvideofonlite.domain.model.DomainError
import pl.sekula.myvideofonlite.domain.model.EventType
import pl.sekula.myvideofonlite.domain.model.LiveViewState
import pl.sekula.myvideofonlite.domain.model.Outcome
import pl.sekula.myvideofonlite.domain.repository.DoorbellRepository
import pl.sekula.myvideofonlite.domain.repository.LiveViewSession

/** Stan ekranu głównego wg tabeli z rozdz. 9 specyfikacji. */
data class HomeState(
    val kind: Kind,
    val device: DoorbellDevice? = null,
    val lastRing: DoorbellEvent? = null,
    val error: String? = null,
) {
    enum class Kind { NOT_CONFIGURED, AUTH_ERROR, ONLINE, OFFLINE, UNKNOWN }

    /** Podgląd dostępny tylko dla urządzenia online, które go wspiera. */
    val canStartLiveView: Boolean
        get() = kind == Kind.ONLINE && device?.supports(Capability.LIVE_VIDEO) == true

    /** Przycisk furtki pokazujemy tylko gdy funkcja jest poprawnie zidentyfikowana. */
    val showGateButton: Boolean
        get() = device?.supports(Capability.GATE_UNLOCK) == true

    val canOpenGate: Boolean
        get() = showGateButton && kind == Kind.ONLINE
}

object ResolveHomeState {
    operator fun invoke(
        device: DoorbellDevice?,
        auth: AuthState,
        events: List<DoorbellEvent>,
        error: String?,
    ): HomeState {
        val lastRing = events.filter { it.type == EventType.RING && it.deviceId == device?.id }
            .maxByOrNull { it.timestampMillis }
        if (auth is AuthState.Expired) return HomeState(HomeState.Kind.AUTH_ERROR, device, lastRing, error)
        if (device == null) return HomeState(HomeState.Kind.NOT_CONFIGURED, error = error)
        val kind = when (device.status) {
            DeviceStatus.ONLINE -> HomeState.Kind.ONLINE
            DeviceStatus.OFFLINE -> HomeState.Kind.OFFLINE
            DeviceStatus.UNKNOWN -> HomeState.Kind.UNKNOWN
        }
        return HomeState(kind, device, lastRing, error)
    }
}

class ObserveHomeState(private val repository: DoorbellRepository) {
    operator fun invoke(): Flow<HomeState> = combine(
        repository.device,
        repository.authState,
        repository.events,
        repository.lastError,
    ) { device, auth, events, error -> ResolveHomeState(device, auth, events, error) }
}

/** Odświeża status urządzenia (ConnectToDoorbell ze specyfikacji). */
class ConnectToDoorbell(private val repository: DoorbellRepository) {
    suspend operator fun invoke(): Outcome<DoorbellDevice> = repository.refreshStatus()
}

/**
 * Otwarcie furtki wymaga świadomego potwierdzenia użytkownika (spec. rozdz. 8).
 * Bez potwierdzenia komenda nigdy nie jest wysyłana.
 */
class OpenGate(private val repository: DoorbellRepository) {
    suspend operator fun invoke(userConfirmed: Boolean): Outcome<Unit> {
        if (!userConfirmed) return Outcome.Failure(DomainError.NOT_CONFIRMED)
        val device = repository.device.first() ?: return Outcome.Failure(DomainError.NOT_CONFIGURED)
        if (!device.supports(Capability.GATE_UNLOCK)) return Outcome.Failure(DomainError.UNSUPPORTED)
        if (device.status != DeviceStatus.ONLINE) return Outcome.Failure(DomainError.DEVICE_OFFLINE)
        return repository.openGate(device.id)
    }
}

/** Tworzy i uruchamia sesję podglądu albo zwraca czytelny błąd. */
class StartLiveView(private val repository: DoorbellRepository) {
    suspend operator fun invoke(): Outcome<LiveViewSession> {
        val device = repository.device.first() ?: return Outcome.Failure(DomainError.NOT_CONFIGURED)
        if (!device.supports(Capability.LIVE_VIDEO)) return Outcome.Failure(DomainError.UNSUPPORTED)
        if (device.status == DeviceStatus.OFFLINE) return Outcome.Failure(DomainError.DEVICE_OFFLINE)
        val session = repository.createLiveSession(device.id)
        session.connect()
        return when (val s = session.state.value) {
            is LiveViewState.Error -> Outcome.Failure(s.error, s.message)
            else -> Outcome.Success(session)
        }
    }
}

/** Decyzja o powiadomieniu po zdarzeniu z urządzenia. */
data class RingNotification(val deviceName: String, val timestampMillis: Long)

class HandleDoorbellEvent(private val repository: DoorbellRepository) {
    suspend operator fun invoke(event: DoorbellEvent): RingNotification? {
        repository.recordEvent(event)
        if (event.type != EventType.RING) return null
        val device = repository.device.first()
        val name = device?.takeIf { it.id == event.deviceId }?.name ?: "Wideodomofon"
        return RingNotification(name, event.timestampMillis)
    }
}
