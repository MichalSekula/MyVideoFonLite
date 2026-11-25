package pl.sekula.myvideofonlite.data.tuya

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OperationDelegateCallBack
import com.thingclips.smart.camera.ipccamerasdk.p2p.ICameraP2P
import com.thingclips.smart.camera.middleware.p2p.IThingSmartCameraP2P
import com.thingclips.smart.camera.middleware.widget.AbsVideoViewCallback
import com.thingclips.smart.camera.middleware.widget.ThingCameraView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.sekula.myvideofonlite.domain.model.DomainError
import pl.sekula.myvideofonlite.domain.model.LiveViewState
import pl.sekula.myvideofonlite.domain.model.Outcome
import pl.sekula.myvideofonlite.domain.repository.LiveViewSession
import kotlin.coroutines.resume

/**
 * Podgląd na żywo i rozmowa przez Tuya IPC SDK (połączenie P2P).
 *
 * Kolejność zgodna z dokumentacją IPC SDK: połączenie P2P, potem widok
 * [ThingCameraView] (podpinany przez [attachRenderTarget]), potem startPreview.
 */
internal class TuyaLiveSession(
    private val deviceId: String,
    private val ready: () -> Outcome.Failure?,
    /** Ładuje urządzenie do pamięci SDK; zwraca online (true/false) albo null, gdy go brak. */
    private val prepare: suspend () -> Boolean?,
) : LiveViewSession {

    private val _state = MutableStateFlow<LiveViewState>(LiveViewState.Idle)
    override val state: StateFlow<LiveViewState> = _state.asStateFlow()

    private var camera: IThingSmartCameraP2P<Any?>? = null
    private var cameraView: ThingCameraView? = null
    private var audioEnabled = false
    private var talking = false

    override suspend fun connect() {
        ready()?.let {
            _state.value = LiveViewState.Error(it.error, it.message)
            return
        }
        _state.value = LiveViewState.Connecting
        val online = prepare()
        if (online == null) {
            _state.value = LiveViewState.Error(DomainError.CONNECTION, "Nie znaleziono domofonu na koncie. Odśwież listę urządzeń.")
            return
        }
        if (!online) {
            _state.value = LiveViewState.Error(DomainError.DEVICE_OFFLINE, "Domofon jest offline. Sprawdź, czy ma zasilanie i Wi-Fi.")
            return
        }
        val core = ThingIPCSdk.getCameraInstance()
        if (core == null || !core.isIPCDevice(deviceId)) {
            _state.value = LiveViewState.Error(DomainError.UNSUPPORTED, "To urządzenie nie obsługuje podglądu przez Tuya IPC SDK.")
            return
        }
        val p2pType = runCatching { core.getP2PType(deviceId) }.getOrDefault(-1)
        val lowPower = runCatching { core.isLowPowerDevice(deviceId) }.getOrDefault(false)
        @Suppress("UNCHECKED_CAST")
        val cam = core.createCameraP2P(deviceId) as IThingSmartCameraP2P<Any?>?
        if (cam == null) {
            _state.value = LiveViewState.Error(DomainError.CONNECTION, "Nie udało się utworzyć sesji kamery (P2P typ $p2pType).")
            return
        }
        camera = cam
        // Kilka prób: tryb automatyczny, potem przez internet (urządzenie może spać albo budzić się po dzwonku).
        val attempts = listOf(
            IThingSmartCameraP2P.CONNECT_MODE_AUTO,
            IThingSmartCameraP2P.CONNECT_MODE_AUTO,
            IThingSmartCameraP2P.CONNECT_MODE_FROM_INTERNET,
        )
        val codes = mutableListOf<Int>()
        for ((index, mode) in attempts.withIndex()) {
            if (camera !== cam) return
            if (index > 0) delay(RETRY_DELAY_MS)
            val error = call { cb -> cam.connect(deviceId, mode, cb) } ?: run {
                _state.value = LiveViewState.Connected(audioEnabled = audioEnabled, talking = false)
                return
            }
            codes += error
            Log.w(TAG, "P2P connect nieudany: kod $error, tryb $mode, typ P2P $p2pType, lowPower $lowPower")
        }
        _state.value = LiveViewState.Error(
            DomainError.CONNECTION,
            "Nie udało się połączyć z domofonem (kody ${codes.joinToString()}; P2P typ $p2pType" +
                (if (lowPower) ", niskie zużycie energii" else "") + ").",
        )
    }

    override fun attachRenderTarget(target: Any?) {
        val cam = camera ?: return
        if (target == null) {
            runCatching { cam.stopPreview(NoOp) }
            cameraView?.onPause()
            cameraView = null
            return
        }
        val container = target as? FrameLayout ?: return
        val view = ThingCameraView(container.context)
        container.removeAllViews()
        container.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        view.setViewCallback(object : AbsVideoViewCallback() {
            override fun onCreated(o: Any?) {
                cam.generateCameraView(o)
                cam.startPreview(object : OperationDelegateCallBack {
                    override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
                        applyMute(cam)
                    }
                    override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                        _state.value = LiveViewState.Error(DomainError.CONNECTION, "Nie udało się uruchomić obrazu (kod $errCode).")
                    }
                })
            }
        })
        view.createVideoView(deviceId)
        view.onResume()
        cameraView = view
    }

    override suspend fun setAudioEnabled(enabled: Boolean) {
        val cam = camera ?: return
        audioEnabled = enabled
        applyMute(cam)
        publishConnected()
    }

    override suspend fun setTalking(talking: Boolean) {
        val cam = camera ?: return
        val error = if (talking) {
            call { cb -> cam.startAudioTalk(cb) }
        } else {
            call { cb -> cam.stopAudioTalk(cb) }
        }
        if (error == null) this.talking = talking
        // Mówienie z wyłączonym głośnikiem nie ma sensu – włączamy dźwięk razem z rozmową.
        if (error == null && talking && !audioEnabled) {
            audioEnabled = true
            applyMute(cam)
        }
        publishConnected()
    }

    override suspend fun disconnect() {
        val cam = camera ?: run {
            _state.value = LiveViewState.Disconnected
            return
        }
        camera = null
        runCatching {
            if (talking) cam.stopAudioTalk(NoOp)
            cam.stopPreview(NoOp)
            cam.disconnect(NoOp)
            cam.destroyP2P()
        }
        cameraView?.onPause()
        cameraView = null
        talking = false
        _state.value = LiveViewState.Disconnected
    }

    private fun applyMute(cam: IThingSmartCameraP2P<Any?>) {
        runCatching {
            cam.setMute(ICameraP2P.PLAYMODE.LIVE, if (audioEnabled) ICameraP2P.UNMUTE else ICameraP2P.MUTE, NoOp)
        }
    }

    private fun publishConnected() {
        if (_state.value is LiveViewState.Connected) {
            _state.value = LiveViewState.Connected(audioEnabled = audioEnabled, talking = talking)
        }
    }

    /** Wywołanie SDK z callbackiem; zwraca null przy sukcesie albo kod błędu. */
    private suspend fun call(block: (OperationDelegateCallBack) -> Unit): Int? =
        suspendCancellableCoroutine { cont ->
            block(object : OperationDelegateCallBack {
                override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
                    if (cont.isActive) cont.resume(null)
                }
                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                    if (cont.isActive) cont.resume(errCode)
                }
            })
        }

    private companion object {
        const val TAG = "TuyaLiveSession"
        const val RETRY_DELAY_MS = 1_500L
    }

    private object NoOp : OperationDelegateCallBack {
        override fun onSuccess(sessionId: Int, requestId: Int, data: String?) = Unit
        override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) = Unit
    }
}
