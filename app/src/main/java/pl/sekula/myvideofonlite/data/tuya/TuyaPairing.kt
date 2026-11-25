package pl.sekula.myvideofonlite.data.tuya

import android.content.Context
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.builder.ActivatorBuilder
import com.thingclips.smart.home.sdk.builder.ThingCameraActivatorBuilder
import com.thingclips.smart.home.sdk.builder.ThingQRCodeActivatorBuilder
import com.thingclips.smart.sdk.api.IThingDataCallback
import com.thingclips.smart.sdk.bean.QrScanBean
import org.json.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingGetHomeListCallback
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.smart.sdk.api.IThingActivatorGetToken
import com.thingclips.smart.sdk.api.IThingSmartActivatorListener
import com.thingclips.smart.sdk.api.IThingSmartCameraActivatorListener
import com.thingclips.smart.sdk.bean.DeviceBean
import com.thingclips.smart.sdk.enums.ActivatorModelEnum
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Sposób parowania obsługiwany przez Tuya SDK dla urządzeń Wi-Fi / kamer. */
enum class PairingMode {
    /** Szybkie miganie diody (EZ / SmartConfig). */
    EZ,

    /** Wolne miganie: telefon łączy się z hotspotem urządzenia (AP). */
    AP,

    /** Urządzenie skanuje kod QR wyświetlony na telefonie (kamery). */
    QR,
}

sealed interface PairingEvent {
    data class QrCode(val content: String) : PairingEvent
    data class Step(val text: String) : PairingEvent
    data class Paired(val deviceId: String, val name: String) : PairingEvent
    data class Failed(val message: String) : PairingEvent
}

/**
 * Parowanie urządzenia z kontem MyVideoFonLite przez oficjalne Tuya SDK.
 * Hasło Wi-Fi trafia tylko do SDK (urządzenia) – nie jest zapisywane ani logowane.
 */
class TuyaPairing(context: Context, private val backend: TuyaBackend) {
    private val appContext = context.applicationContext

    fun pair(mode: PairingMode, ssid: String, wifiPassword: String): Flow<PairingEvent> = callbackFlow {
        backend.ensureReady()?.let {
            trySend(PairingEvent.Failed(it))
            close()
            return@callbackFlow
        }
        trySend(PairingEvent.Step("Przygotowuję dom i token parowania…"))
        val homeId = homeId()
        if (homeId == null) {
            trySend(PairingEvent.Failed("Nie udało się utworzyć ani pobrać domu na koncie."))
            close()
            return@callbackFlow
        }
        val token = token(homeId)
        if (token == null) {
            trySend(PairingEvent.Failed("Nie udało się pobrać tokenu parowania z Tuya."))
            close()
            return@callbackFlow
        }

        fun success(bean: DeviceBean?) {
            backend.invalidateCache()
            trySend(PairingEvent.Paired(bean?.devId.orEmpty(), bean?.name ?: "Domofon"))
            close()
        }

        fun failure(code: String?, msg: String?) {
            trySend(PairingEvent.Failed("Parowanie nieudane: ${msg ?: "nieznany błąd"} (${code ?: "?"})."))
            close()
        }

        val stop: () -> Unit = when (mode) {
            PairingMode.QR -> {
                val activator = ThingHomeSdk.getActivatorInstance().newCameraDevActivator(
                    ThingCameraActivatorBuilder()
                        .setContext(appContext)
                        .setSsid(ssid)
                        .setPassword(wifiPassword)
                        .setToken(token)
                        .setTimeOut(TIMEOUT_SECONDS)
                        .setListener(object : IThingSmartCameraActivatorListener {
                            override fun onQRCodeSuccess(qrcodeUrl: String?) {
                                if (qrcodeUrl != null) trySend(PairingEvent.QrCode(qrcodeUrl))
                            }
                            override fun onError(errorCode: String?, errorMsg: String?) = failure(errorCode, errorMsg)
                            override fun onActiveSuccess(devResp: DeviceBean?) = success(devResp)
                        }),
                )
                activator.createQRCode()
                activator.start()
                trySend(PairingEvent.Step("Pokaż kod QR kamerze domofonu z odległości ok. 15–20 cm."))
                ({ runCatching { activator.stop(); activator.onDestroy() } })
            }
            PairingMode.EZ, PairingMode.AP -> {
                val activator = ThingHomeSdk.getActivatorInstance().newActivator(
                    ActivatorBuilder()
                        .setContext(appContext)
                        .setSsid(ssid)
                        .setPassword(wifiPassword)
                        .setToken(token)
                        .setTimeOut(TIMEOUT_SECONDS)
                        .setActivatorModel(if (mode == PairingMode.EZ) ActivatorModelEnum.THING_EZ else ActivatorModelEnum.THING_AP)
                        .setListener(object : IThingSmartActivatorListener {
                            override fun onError(errorCode: String?, errorMsg: String?) = failure(errorCode, errorMsg)
                            override fun onActiveSuccess(devResp: DeviceBean?) = success(devResp)
                            override fun onStep(step: String?, data: Any?) {
                                val text = when (step) {
                                    "device_find" -> "Znaleziono urządzenie…"
                                    "device_bind_success" -> "Urządzenie połączone, rejestruję na koncie…"
                                    else -> null
                                }
                                if (text != null) trySend(PairingEvent.Step(text))
                            }
                        }),
                )
                activator.start()
                trySend(PairingEvent.Step("Szukam urządzenia (do ${TIMEOUT_SECONDS / 60} min)…"))
                ({ runCatching { activator.stop(); activator.onDestroy() } })
            }
        }
        awaitClose { stop() }
    }

    /**
     * Urządzenie z ekranem pokazuje kod QR, a telefon go skanuje.
     * Urządzenie musi już być w sieci Wi-Fi – Tuya przypisuje je do konta po identyfikatorze z kodu.
     */
    fun pairScannedCode(scanned: String): Flow<PairingEvent> = callbackFlow {
        backend.ensureReady()?.let {
            trySend(PairingEvent.Failed(it))
            close()
            return@callbackFlow
        }
        trySend(PairingEvent.Step("Odczytuję kod z urządzenia…"))
        val uuid = parseUuid(scanned)
        if (uuid == null) {
            trySend(PairingEvent.Failed("To nie jest kod parowania Tuya. Zeskanuj kod z ekranu domofonu."))
            close()
            return@callbackFlow
        }
        val homeId = homeId()
        if (homeId == null) {
            trySend(PairingEvent.Failed("Nie udało się utworzyć ani pobrać domu na koncie."))
            close()
            return@callbackFlow
        }
        trySend(PairingEvent.Step("Dodaję domofon do konta…"))
        val activator = ThingHomeSdk.getActivatorInstance().newQRCodeDevActivator(
            ThingQRCodeActivatorBuilder()
                .setUuid(uuid)
                .setHomeId(homeId)
                .setContext(appContext)
                .setTimeOut(TIMEOUT_SECONDS)
                .setListener(object : IThingSmartActivatorListener {
                    override fun onError(errorCode: String?, errorMsg: String?) {
                        trySend(PairingEvent.Failed("Parowanie nieudane: ${errorMsg ?: "nieznany błąd"} (${errorCode ?: "?"})."))
                        close()
                    }
                    override fun onActiveSuccess(devResp: DeviceBean?) {
                        backend.invalidateCache()
                        trySend(PairingEvent.Paired(devResp?.devId.orEmpty(), devResp?.name ?: "Domofon"))
                        close()
                    }
                    override fun onStep(step: String?, data: Any?) = Unit
                }),
        )
        activator.start()
        awaitClose { runCatching { activator.stop(); activator.onDestroy() } }
    }

    /** Identyfikator urządzenia z kodu: przez parser Tuya, a awaryjnie z parametru `uuid` w adresie. */
    private suspend fun parseUuid(scanned: String): String? {
        val bean = suspendCancellableCoroutine<QrScanBean?> { cont ->
            ThingHomeSdk.getActivatorInstance().deviceQrCodeParse(scanned, object : IThingDataCallback<QrScanBean> {
                override fun onSuccess(result: QrScanBean?) { if (cont.isActive) cont.resume(result) }
                override fun onError(errorCode: String?, errorMessage: String?) { if (cont.isActive) cont.resume(null) }
            })
        }
        val data = bean?.actionData
        val fromBean = when (data) {
            is Map<*, *> -> data["uuid"]?.toString()
            null -> null
            else -> runCatching { JSONObject(data.toString()).optString("uuid") }.getOrNull()
        }?.takeIf { it.isNotBlank() }
        return fromBean ?: Regex("[?&]uuid=([^&]+)").find(scanned)?.groupValues?.get(1)
    }

    private suspend fun homeId(): Long? {
        val homes = suspendCancellableCoroutine<List<HomeBean>?> { cont ->
            ThingHomeSdk.getHomeManagerInstance().queryHomeList(object : IThingGetHomeListCallback {
                override fun onSuccess(homeBeans: MutableList<HomeBean>?) { if (cont.isActive) cont.resume(homeBeans.orEmpty()) }
                override fun onError(code: String?, error: String?) { if (cont.isActive) cont.resume(null) }
            })
        } ?: return null
        homes.firstOrNull()?.let { return it.homeId }
        return suspendCancellableCoroutine { cont ->
            ThingHomeSdk.getHomeManagerInstance().createHome(
                "Dom", 0.0, 0.0, "", listOf("Wejście"),
                object : IThingHomeResultCallback {
                    override fun onSuccess(bean: HomeBean?) { if (cont.isActive) cont.resume(bean?.homeId) }
                    override fun onError(code: String?, error: String?) { if (cont.isActive) cont.resume(null) }
                },
            )
        }
    }

    private suspend fun token(homeId: Long): String? = suspendCancellableCoroutine { cont ->
        ThingHomeSdk.getActivatorInstance().getActivatorToken(homeId, object : IThingActivatorGetToken {
            override fun onSuccess(token: String?) { if (cont.isActive) cont.resume(token) }
            override fun onFailure(code: String?, error: String?) { if (cont.isActive) cont.resume(null) }
        })
    }

    private companion object {
        const val TIMEOUT_SECONDS = 120L
    }
}
