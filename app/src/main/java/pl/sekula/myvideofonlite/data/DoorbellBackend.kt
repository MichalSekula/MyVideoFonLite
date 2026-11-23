package pl.sekula.myvideofonlite.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pl.sekula.myvideofonlite.domain.model.AuthState
import pl.sekula.myvideofonlite.domain.model.DeviceSummary
import pl.sekula.myvideofonlite.domain.model.DoorbellDevice
import pl.sekula.myvideofonlite.domain.model.DoorbellEvent
import pl.sekula.myvideofonlite.domain.model.Outcome
import pl.sekula.myvideofonlite.domain.repository.LiveViewSession

/**
 * Źródło danych o urządzeniu: symulator (demo) albo oficjalne SDK Tuya.
 * Implementacje mapują odpowiedzi SDK na modele domenowe (spec. 6.3 – Data).
 */
interface DoorbellBackend {
    val authState: StateFlow<AuthState>

    /** Zdarzenia z urządzenia (dzwonek, ruch) dostarczane przez SDK/symulator. */
    val incomingEvents: Flow<DoorbellEvent>

    suspend fun signIn(countryCode: String, email: String, password: String): Outcome<Unit>
    suspend fun signOut()
    suspend fun listDevices(): Outcome<List<DeviceSummary>>
    suspend fun fetchDevice(deviceId: String): Outcome<DoorbellDevice>
    suspend fun openGate(deviceId: String): Outcome<Unit>
    fun createLiveSession(deviceId: String): LiveViewSession
}
