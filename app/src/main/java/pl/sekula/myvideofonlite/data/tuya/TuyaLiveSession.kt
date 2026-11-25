package pl.sekula.myvideofonlite.data.tuya

import android.view.ViewGroup
import android.widget.FrameLayout
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OperationDelegateCallBack
import com.thingclips.smart.camera.ipccamerasdk.p2p.ICameraP2P
import com.thingclips.smart.camera.middleware.p2p.IThingSmartCameraP2P
import com.thingclips.smart.camera.middleware.widget.AbsVideoViewCallback
import com.thingclips.smart.camera.middleware.widget.ThingCameraView
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
        val core = ThingIPCSdk.getCameraInstance()
        if (core == null || !core.isIPCDevice(deviceId)) {
            _state.value = LiveViewState.Error(DomainError.UNSUPPORTED, "To urządzenie nie obsługuje podglądu przez Tuya IPC SDK.")
            return
        }
        _state.value = LiveViewState.Connecting
        @Suppress("UNCHECKED_CAST")
        val cam = core.createCameraP2P(deviceId) as IThingSmartCameraP2P<Any?>?
        if (cam == null) {
            _state.value = LiveViewState.Error(DomainError.CONNECTION, "Nie udało się utworzyć sesji kamery.")
            return
        }
        camera = cam
        val error = call { cb -> cam.connect(deviceId, cb) }
        _state.value = if (error == null) {
            LiveViewState.Connected(audioEnabled = audioEnabled, talking = false)
        } else {
            LiveViewState.Error(DomainError.CONNECTION, "Nie udało się połączyć z domofonem (kod $error).")
        }
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

    private object NoOp : OperationDelegateCallBack {
        override fun onSuccess(sessionId: Int, requestId: Int, data: String?) = Unit
        override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) = Unit
    }
}
