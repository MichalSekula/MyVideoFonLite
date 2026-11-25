package pl.sekula.myvideofonlite.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.sekula.myvideofonlite.data.tuya.PairingEvent
import pl.sekula.myvideofonlite.data.tuya.PairingMode
import pl.sekula.myvideofonlite.data.tuya.TuyaPairing
import pl.sekula.myvideofonlite.domain.repository.SettingsRepository
import pl.sekula.myvideofonlite.domain.usecase.ConnectToDoorbell

data class PairingUiState(
    val running: Boolean = false,
    val status: String? = null,
    val error: String? = null,
    val pairedName: String? = null,
)

class PairingViewModel(
    private val pairing: TuyaPairing,
    private val settings: SettingsRepository,
    private val connectToDoorbell: ConnectToDoorbell,
) : ViewModel() {

    private val _state = MutableStateFlow(PairingUiState())
    val state: StateFlow<PairingUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun pairScanned(code: String) = run(pairing.pairScannedCode(code))

    fun pairWifi(mode: PairingMode, ssid: String, password: String) = run(pairing.pair(mode, ssid, password))

    fun cancel() {
        job?.cancel()
        _state.value = PairingUiState(status = "Przerwano parowanie.")
    }

    fun scanFailed(message: String) {
        _state.value = PairingUiState(error = message)
    }

    private fun run(events: Flow<PairingEvent>) {
        job?.cancel()
        _state.value = PairingUiState(running = true)
        job = viewModelScope.launch {
            events.collect { event ->
                when (event) {
                    is PairingEvent.Step -> _state.value = _state.value.copy(status = event.text)
                    is PairingEvent.QrCode -> _state.value = _state.value.copy(status = "Kod QR gotowy.")
                    is PairingEvent.Failed -> _state.value = PairingUiState(error = event.message)
                    is PairingEvent.Paired -> {
                        if (event.deviceId.isNotBlank()) {
                            settings.setSelectedDevice(event.deviceId)
                            connectToDoorbell()
                        }
                        _state.value = PairingUiState(pairedName = event.name)
                    }
                }
            }
            if (_state.value.running) _state.value = _state.value.copy(running = false)
        }
    }
}
