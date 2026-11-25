package pl.sekula.myvideofonlite.data.tuya

import android.app.Application
import android.content.Context
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.android.user.api.ILoginCallback
import com.thingclips.smart.android.user.api.ILogoutCallback
import com.thingclips.smart.android.user.api.IRegisterCallback
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.callback.IThingGetHomeListCallback
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.smart.sdk.api.IDevListener
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDevice
import com.thingclips.smart.sdk.bean.DeviceBean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import pl.sekula.myvideofonlite.data.DoorbellBackend
import pl.sekula.myvideofonlite.domain.model.AuthState
import pl.sekula.myvideofonlite.domain.model.Capability
import pl.sekula.myvideofonlite.domain.model.DeviceStatus
import pl.sekula.myvideofonlite.domain.model.DeviceSummary
import pl.sekula.myvideofonlite.domain.model.DoorbellDevice
import pl.sekula.myvideofonlite.domain.model.DoorbellEvent
import pl.sekula.myvideofonlite.domain.model.DomainError
import pl.sekula.myvideofonlite.domain.model.EventType
import pl.sekula.myvideofonlite.domain.model.Outcome
import pl.sekula.myvideofonlite.domain.repository.LiveViewSession
import kotlin.coroutines.resume

/**
 * Integracja z oficjalnym Tuya Smart Life App SDK (konto, dom, urządzenia, DP)
 * oraz Tuya IPC SDK (P2P, wideo, audio) – patrz [TuyaLiveSession].
 *
 * Zasady: żadnych nieudokumentowanych endpointów, żadnych sekretów Cloud OpenAPI
 * w aplikacji, żadnego logowania tokenów ani haseł.
 */
class TuyaBackend(context: Context) : DoorbellBackend {

    private val app: Application = context.applicationContext as Application

    /** true, gdy build ma klucze aplikacji Tuya SDK. */
    val hasSdkKeys: Boolean = TuyaSdk.hasKeys

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState

    private val _events = MutableSharedFlow<DoorbellEvent>(extraBufferCapacity = 8)
    override val incomingEvents: Flow<DoorbellEvent> = _events.asSharedFlow()

    /** Ostatnio pobrane urządzenia z domów użytkownika (po identyfikatorze). */
    private val deviceCache = mutableMapOf<String, DeviceBean>()

    private var listenedDevice: IThingDevice? = null
    private var listenedDeviceId: String? = null

    /** Uruchamia SDK i odtwarza zapisaną sesję. Wołane przy starcie w trybie Tuya. */
    fun start(): Boolean {
        if (!TuyaSdk.ensureInit(app)) return false
        val user = ThingHomeSdk.getUserInstance()
        if (user.isLogin) {
            _authState.value = AuthState.SignedIn(user.user?.email?.takeIf { it.isNotBlank() } ?: "Konto Tuya")
        }
        ThingHomeSdk.setOnNeedLoginListener { _authState.value = AuthState.Expired }
        return true
    }

    private fun ready(): Outcome.Failure? = when {
        !hasSdkKeys -> Outcome.Failure(DomainError.NOT_CONFIGURED, NO_KEYS_MESSAGE)
        !start() -> Outcome.Failure(
            DomainError.NOT_CONFIGURED,
            INIT_FAILED_MESSAGE + (TuyaSdk.lastError?.let { "\n\nSzczegóły: $it" } ?: ""),
        )
        else -> null
    }

    override suspend fun signIn(countryCode: String, email: String, password: String): Outcome<Unit> {
        ready()?.let { return it }
        val code = countryCode.trim().removePrefix("+").ifBlank { "48" }
        return suspendCancellableCoroutine { cont ->
            ThingHomeSdk.getUserInstance().loginWithEmail(code, email.trim(), password, object : ILoginCallback {
                override fun onSuccess(user: User?) {
                    _authState.value = AuthState.SignedIn(user?.email?.takeIf { it.isNotBlank() } ?: email.trim())
                    if (cont.isActive) cont.resume(Outcome.Success(Unit))
                }

                override fun onError(errorCode: String?, errorMsg: String?) {
                    if (cont.isActive) cont.resume(Outcome.Failure(DomainError.AUTH, loginErrorMessage(errorCode, errorMsg)))
                }
            })
        }
    }

    override suspend fun requestRegistrationCode(countryCode: String, email: String): Outcome<Unit> {
        ready()?.let { return it }
        return suspendCancellableCoroutine { cont ->
            // Aktualna metoda SDK (typ 1 = rejestracja); region pusty = wynikający z kodu kraju.
            ThingHomeSdk.getUserInstance().sendVerifyCodeWithUserName(email.trim(), "", country(countryCode), 1, object : IResultCallback {
                override fun onSuccess() { if (cont.isActive) cont.resume(Outcome.Success(Unit)) }
                override fun onError(code: String?, error: String?) {
                    if (cont.isActive) cont.resume(
                        Outcome.Failure(DomainError.CONNECTION, "Nie udało się wysłać kodu: ${error ?: "nieznany błąd"} (${code ?: "?"}).")
                    )
                }
            })
        }
    }

    override suspend fun register(countryCode: String, email: String, password: String, code: String): Outcome<Unit> {
        ready()?.let { return it }
        return suspendCancellableCoroutine { cont ->
            ThingHomeSdk.getUserInstance().registerAccountWithEmail(
                country(countryCode), email.trim(), password, code.trim(),
                object : IRegisterCallback {
                    override fun onSuccess(user: User?) {
                        _authState.value = AuthState.SignedIn(user?.email?.takeIf { it.isNotBlank() } ?: email.trim())
                        if (cont.isActive) cont.resume(Outcome.Success(Unit))
                    }
                    override fun onError(errorCode: String?, errorMsg: String?) {
                        if (cont.isActive) cont.resume(Outcome.Failure(DomainError.AUTH, loginErrorMessage(errorCode, errorMsg)))
                    }
                },
            )
        }
    }

    private fun country(code: String) = code.trim().removePrefix("+").ifBlank { "48" }

    override suspend fun signOut() {
        stopListening()
        deviceCache.clear()
        if (TuyaSdk.hasKeys && start()) {
            suspendCancellableCoroutine { cont ->
                ThingHomeSdk.getUserInstance().logout(object : ILogoutCallback {
                    override fun onSuccess() { if (cont.isActive) cont.resume(Unit) }
                    override fun onError(code: String?, error: String?) { if (cont.isActive) cont.resume(Unit) }
                })
            }
        }
        _authState.value = AuthState.SignedOut
    }

    override suspend fun listDevices(): Outcome<List<DeviceSummary>> {
        ready()?.let { return it }
        if (_authState.value !is AuthState.SignedIn) return Outcome.Failure(DomainError.AUTH, "Najpierw zaloguj się kontem Tuya.")
        return when (val r = loadAllDevices()) {
            is Outcome.Failure -> r
            is Outcome.Success -> {
                val all = r.value
                // Wideodomofon to w Tuya kamera (IPC). Gdy żadnej nie ma, pokazujemy wszystko.
                val cameras = all.filter { isCamera(it) }
                Outcome.Success((cameras.ifEmpty { all }).map { DeviceSummary(it.devId, it.name ?: it.devId) })
            }
        }
    }

    override suspend fun fetchDevice(deviceId: String): Outcome<DoorbellDevice> {
        ready()?.let { return it }
        if (_authState.value !is AuthState.SignedIn) return Outcome.Failure(DomainError.AUTH)
        val bean = findDevice(deviceId) ?: return Outcome.Failure(
            DomainError.NOT_CONFIGURED,
            "Nie znaleziono urządzenia na koncie Tuya. Wybierz je ponownie w ustawieniach.",
        )
        listenTo(deviceId)
        return Outcome.Success(bean.toDomain())
    }

    override suspend fun openGate(deviceId: String): Outcome<Unit> {
        ready()?.let { return it }
        val bean = findDevice(deviceId) ?: return Outcome.Failure(DomainError.NOT_CONFIGURED)
        if (bean.isOnline != true) return Outcome.Failure(DomainError.DEVICE_OFFLINE)
        val dpId = DpMap.gateDpId(bean)
            ?: return Outcome.Failure(DomainError.UNSUPPORTED, "Urządzenie nie udostępnia funkcji otwarcia furtki.")
        val command = JSONObject().put(dpId, true).toString()
        return suspendCancellableCoroutine { cont ->
            ThingHomeSdk.newDeviceInstance(deviceId).publishDps(command, object : IResultCallback {
                override fun onSuccess() { if (cont.isActive) cont.resume(Outcome.Success(Unit)) }
                override fun onError(code: String?, error: String?) {
                    if (cont.isActive) cont.resume(
                        Outcome.Failure(DomainError.CONNECTION, "Nie udało się otworzyć furtki (${code ?: "?"}).")
                    )
                }
            })
        }
    }

    override fun createLiveSession(deviceId: String): LiveViewSession = TuyaLiveSession(deviceId) { ready() }

    /** null, gdy SDK gotowe i użytkownik zalogowany; w przeciwnym razie komunikat. */
    fun ensureReady(): String? {
        ready()?.let { return it.message ?: "Tuya SDK nie jest gotowe." }
        return if (_authState.value is AuthState.SignedIn) null else "Najpierw zaloguj się lub załóż konto w ustawieniach."
    }

    fun invalidateCache() = deviceCache.clear()

    // --- Pomocnicze ---

    private suspend fun findDevice(deviceId: String): DeviceBean? {
        val fromSdk = runCatching { ThingHomeSdk.getDataInstance().getDeviceBean(deviceId) }.getOrNull()
        if (fromSdk != null) return fromSdk.also { deviceCache[deviceId] = it }
        deviceCache[deviceId]?.let { return it }
        loadAllDevices()
        return deviceCache[deviceId]
    }

    private suspend fun loadAllDevices(): Outcome<List<DeviceBean>> {
        val homes = suspendCancellableCoroutine<Outcome<List<HomeBean>>> { cont ->
            ThingHomeSdk.getHomeManagerInstance().queryHomeList(object : IThingGetHomeListCallback {
                override fun onSuccess(homeBeans: MutableList<HomeBean>?) {
                    if (cont.isActive) cont.resume(Outcome.Success(homeBeans.orEmpty()))
                }
                override fun onError(code: String?, error: String?) {
                    if (cont.isActive) cont.resume(Outcome.Failure(errorFor(code), "Nie udało się pobrać listy domów (${code ?: "?"})."))
                }
            })
        }
        if (homes is Outcome.Failure) return homes
        val devices = mutableListOf<DeviceBean>()
        for (home in (homes as Outcome.Success).value) {
            val detail = suspendCancellableCoroutine<HomeBean?> { cont ->
                ThingHomeSdk.newHomeInstance(home.homeId).getHomeDetail(object : IThingHomeResultCallback {
                    override fun onSuccess(bean: HomeBean?) { if (cont.isActive) cont.resume(bean) }
                    override fun onError(code: String?, error: String?) { if (cont.isActive) cont.resume(null) }
                })
            } ?: continue
            devices += detail.deviceList.orEmpty()
            devices += detail.sharedDeviceList.orEmpty()
        }
        val unique = devices.distinctBy { it.devId }
        unique.forEach { deviceCache[it.devId] = it }
        return Outcome.Success(unique)
    }

    private fun isCamera(bean: DeviceBean): Boolean =
        bean.category == "sp" || runCatching { ThingIPCSdk.getCameraInstance()?.isIPCDevice(bean.devId) == true }.getOrDefault(false)

    private fun DeviceBean.toDomain(): DoorbellDevice {
        val caps = buildSet {
            if (isCamera(this@toDomain)) {
                add(Capability.LIVE_VIDEO)
                add(Capability.TWO_WAY_AUDIO)
            }
            if (DpMap.ringDpId(this@toDomain) != null) add(Capability.DOORBELL_EVENTS)
            if (DpMap.gateDpId(this@toDomain) != null) add(Capability.GATE_UNLOCK)
        }
        val status = when (isOnline) {
            true -> DeviceStatus.ONLINE
            false -> DeviceStatus.OFFLINE
            null -> DeviceStatus.UNKNOWN
        }
        return DoorbellDevice(id = devId, name = name ?: devId, status = status, capabilities = caps)
    }

    /** Nasłuch zmian DP wybranego urządzenia: dzwonek i ruch trafiają do [incomingEvents]. */
    private fun listenTo(deviceId: String) {
        if (listenedDeviceId == deviceId) return
        stopListening()
        val device = ThingHomeSdk.newDeviceInstance(deviceId)
        device.registerDevListener(object : IDevListener {
            override fun onDpUpdate(devId: String?, dpStr: String?) {
                val bean = deviceCache[deviceId] ?: return
                val changed = runCatching { JSONObject(dpStr ?: return).keys().asSequence().toSet() }.getOrNull() ?: return
                val type = when {
                    DpMap.ringDpId(bean)?.let { it in changed } == true -> EventType.RING
                    DpMap.motionDpId(bean)?.let { it in changed } == true -> EventType.MOTION
                    else -> return
                }
                _events.tryEmit(DoorbellEvent(deviceId, type, System.currentTimeMillis()))
            }
            override fun onRemoved(devId: String?) = Unit
            override fun onStatusChanged(devId: String?, online: Boolean) {
                deviceCache[deviceId]?.isOnline = online
            }
            override fun onNetworkStatusChanged(devId: String?, status: Boolean) = Unit
            override fun onDevInfoUpdate(devId: String?) = Unit
        })
        listenedDevice = device
        listenedDeviceId = deviceId
    }

    private fun stopListening() {
        runCatching {
            listenedDevice?.unRegisterDevListener()
            listenedDevice?.onDestroy()
        }
        listenedDevice = null
        listenedDeviceId = null
    }

    private fun errorFor(code: String?): DomainError = when (code) {
        "USER_SESSION_LOSS", "USER_SESSION_INVALID", "SING_VALIDATE_FALED" -> DomainError.AUTH
        else -> DomainError.CONNECTION
    }

    private fun loginErrorMessage(code: String?, msg: String?): String {
        val lower = (msg ?: "").lowercase()
        return when {
            "illegal client" in lower || code == "ILLEGAL_CLIENT_ID" ->
                "Tuya odrzuciła aplikację (illegal client). Sprawdź, czy SHA-256 klucza podpisu i nazwa pakietu w panelu Tuya zgadzają się z tą wersją aplikacji."
            else -> "Logowanie nieudane: ${msg ?: "nieznany błąd"} (${code ?: "?"})."
        }
    }

    companion object {
        const val NO_KEYS_MESSAGE =
            "Ta wersja aplikacji nie ma kluczy Tuya. Zbuduj ją z kluczami (sekrety TUYA_APP_KEY i TUYA_APP_SECRET)."
        const val INIT_FAILED_MESSAGE =
            "Nie udało się uruchomić Tuya SDK na tym urządzeniu."
    }
}
