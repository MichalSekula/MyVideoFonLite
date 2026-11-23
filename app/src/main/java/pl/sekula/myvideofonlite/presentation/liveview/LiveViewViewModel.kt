package pl.sekula.myvideofonlite.presentation.liveview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.sekula.myvideofonlite.domain.model.BackendMode
import pl.sekula.myvideofonlite.domain.model.DoorbellDevice
import pl.sekula.myvideofonlite.domain.model.LiveViewState
import pl.sekula.myvideofonlite.domain.model.Outcome
import pl.sekula.myvideofonlite.domain.model.defaultMessage
import pl.sekula.myvideofonlite.domain.repository.DoorbellRepository
import pl.sekula.myvideofonlite.domain.repository.LiveViewSession
import pl.sekula.myvideofonlite.domain.repository.SettingsRepository
import pl.sekula.myvideofonlite.domain.usecase.OpenGate
import pl.sekula.myvideofonlite.domain.usecase.StartLiveView

class LiveViewViewModel(
    repository: DoorbellRepository,
    private val startLiveView: StartLiveView,
    private val openGate: OpenGate,
    settings: SettingsRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    val device: StateFlow<DoorbellDevice?> = repository.device
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isDemo: StateFlow<Boolean> = settings.settings.map { it.backendMode == BackendMode.DEMO }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val requireBiometric: StateFlow<Boolean> = settings.settings.map { it.requireBiometricForGate }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _state = MutableStateFlow<LiveViewState>(LiveViewState.Idle)
    val state: StateFlow<LiveViewState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var session: LiveViewSession? = null
    private var sessionJob: Job? = null

    init {
        connect()
    }

    fun connect() {
        sessionJob?.cancel()
        val previous = session
        session = null
        sessionJob = viewModelScope.launch {
            previous?.disconnect()
            _state.value = LiveViewState.Connecting
            when (val result = startLiveView()) {
                is Outcome.Success -> {
                    session = result.value
                    result.value.state.collect { _state.value = it }
                }
                is Outcome.Failure -> _state.value = LiveViewState.Error(result.error, result.message)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch { session?.disconnect() }
    }

    fun setAudio(enabled: Boolean) {
        viewModelScope.launch { session?.setAudioEnabled(enabled) }
    }

    fun setTalking(talking: Boolean) {
        viewModelScope.launch { session?.setTalking(talking) }
    }

    fun attachRenderTarget(target: Any?) {
        session?.attachRenderTarget(target)
    }

    fun openGateConfirmed() {
        viewModelScope.launch {
            _message.value = when (val r = openGate(userConfirmed = true)) {
                is Outcome.Success -> "Wysłano polecenie otwarcia furtki."
                is Outcome.Failure -> r.message ?: r.error.defaultMessage()
            }
        }
    }

    fun showMessage(text: String) {
        _message.value = text
    }

    fun messageShown() {
        _message.value = null
    }

    override fun onCleared() {
        // Zawsze zamykamy sesję P2P po wyjściu z ekranu.
        val s = session
        appScope.launch { s?.disconnect() }
    }
}
