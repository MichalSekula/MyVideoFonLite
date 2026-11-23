package pl.sekula.myvideofonlite.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import pl.sekula.myvideofonlite.domain.model.AppSettings
import pl.sekula.myvideofonlite.domain.model.BackendMode
import pl.sekula.myvideofonlite.domain.model.Capability
import pl.sekula.myvideofonlite.domain.model.DeviceStatus
import pl.sekula.myvideofonlite.domain.model.DoorbellDevice
import pl.sekula.myvideofonlite.domain.model.DoorbellEvent
import pl.sekula.myvideofonlite.domain.model.EventType
import pl.sekula.myvideofonlite.domain.repository.SettingsRepository
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "myvideofon")

/** Ostatni znany stan dla widgetu – nigdy nie udaje stanu "na żywo". */
data class WidgetSnapshot(
    val configured: Boolean,
    val deviceName: String?,
    val status: DeviceStatus,
    val statusUpdatedAt: Long?,
    val lastRingAt: Long?,
    val gateAvailable: Boolean,
)

/**
 * Trwała konfiguracja i ostatni znany stan (DataStore).
 * Nie przechowujemy tu haseł ani tokenów – sesją zarządza SDK Tuya.
 */
class LocalStore(context: Context) : SettingsRepository {

    private val dataStore = context.applicationContext.dataStore

    private object Keys {
        val MODE = stringPreferencesKey("backend_mode")
        val SELECTED = stringPreferencesKey("selected_device")
        val BIOMETRIC = booleanPreferencesKey("biometric_gate")
        val SNAP_ID = stringPreferencesKey("snap_id")
        val SNAP_NAME = stringPreferencesKey("snap_name")
        val SNAP_STATUS = stringPreferencesKey("snap_status")
        val SNAP_CAPS = stringPreferencesKey("snap_caps")
        val SNAP_UPDATED = longPreferencesKey("snap_updated")
        val EVENTS = stringPreferencesKey("events")
        val LAST_ERROR = stringPreferencesKey("last_error")
    }

    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val settings: Flow<AppSettings> = prefs.map { p ->
        AppSettings(
            backendMode = p[Keys.MODE]?.let { runCatching { BackendMode.valueOf(it) }.getOrNull() }
                ?: BackendMode.DEMO,
            selectedDeviceId = p[Keys.SELECTED],
            requireBiometricForGate = p[Keys.BIOMETRIC] ?: true,
        )
    }.distinctUntilChanged()

    val snapshot: Flow<DoorbellDevice?> = prefs.map { it.toDevice() }.distinctUntilChanged()

    val events: Flow<List<DoorbellEvent>> = prefs.map { decodeEvents(it[Keys.EVENTS]) }.distinctUntilChanged()

    val lastError: Flow<String?> = prefs.map { it[Keys.LAST_ERROR] }.distinctUntilChanged()

    val widgetSnapshot: Flow<WidgetSnapshot> = prefs.map { p ->
        val selected = p[Keys.SELECTED]
        val device = p.toDevice()?.takeIf { it.id == selected }
        WidgetSnapshot(
            configured = device != null,
            deviceName = device?.name,
            status = device?.status ?: DeviceStatus.UNKNOWN,
            statusUpdatedAt = p[Keys.SNAP_UPDATED],
            lastRingAt = decodeEvents(p[Keys.EVENTS])
                .filter { it.type == EventType.RING && it.deviceId == selected }
                .maxOfOrNull { it.timestampMillis },
            gateAvailable = device?.supports(Capability.GATE_UNLOCK) == true,
        )
    }.distinctUntilChanged()

    override suspend fun setBackendMode(mode: BackendMode) {
        dataStore.edit { it[Keys.MODE] = mode.name }
    }

    override suspend fun setSelectedDevice(deviceId: String?) {
        dataStore.edit { if (deviceId == null) it.remove(Keys.SELECTED) else it[Keys.SELECTED] = deviceId }
    }

    override suspend fun setRequireBiometricForGate(required: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC] = required }
    }

    suspend fun saveSnapshot(device: DoorbellDevice) {
        dataStore.edit {
            it[Keys.SNAP_ID] = device.id
            it[Keys.SNAP_NAME] = device.name
            it[Keys.SNAP_STATUS] = device.status.name
            it[Keys.SNAP_CAPS] = device.capabilities.joinToString(",") { c -> c.name }
            it[Keys.SNAP_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun clearSnapshot() {
        dataStore.edit {
            it.remove(Keys.SNAP_ID)
            it.remove(Keys.SNAP_NAME)
            it.remove(Keys.SNAP_STATUS)
            it.remove(Keys.SNAP_CAPS)
            it.remove(Keys.SNAP_UPDATED)
        }
    }

    suspend fun addEvent(event: DoorbellEvent) {
        dataStore.edit { p ->
            val list = (listOf(event) + decodeEvents(p[Keys.EVENTS])).take(MAX_EVENTS)
            p[Keys.EVENTS] = list.joinToString("\n") { "${it.deviceId}|${it.type.name}|${it.timestampMillis}" }
        }
    }

    suspend fun setLastError(message: String?) {
        dataStore.edit { if (message == null) it.remove(Keys.LAST_ERROR) else it[Keys.LAST_ERROR] = message }
    }

    private fun Preferences.toDevice(): DoorbellDevice? {
        val id = this[Keys.SNAP_ID] ?: return null
        return DoorbellDevice(
            id = id,
            name = this[Keys.SNAP_NAME] ?: id,
            status = this[Keys.SNAP_STATUS]?.let { runCatching { DeviceStatus.valueOf(it) }.getOrNull() }
                ?: DeviceStatus.UNKNOWN,
            capabilities = this[Keys.SNAP_CAPS].orEmpty().split(",")
                .mapNotNull { runCatching { Capability.valueOf(it) }.getOrNull() }
                .toSet(),
        )
    }

    private fun decodeEvents(raw: String?): List<DoorbellEvent> =
        raw.orEmpty().lineSequence().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size != 3) return@mapNotNull null
            val type = runCatching { EventType.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            val ts = parts[2].toLongOrNull() ?: return@mapNotNull null
            DoorbellEvent(parts[0], type, ts)
        }.toList()

    private companion object {
        const val MAX_EVENTS = 50
    }
}
