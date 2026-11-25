package pl.sekula.myvideofonlite.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.sekula.myvideofonlite.data.demo.DemoBackend
import pl.sekula.myvideofonlite.domain.model.AppSettings
import pl.sekula.myvideofonlite.domain.model.AuthState
import pl.sekula.myvideofonlite.domain.model.BackendMode
import pl.sekula.myvideofonlite.domain.model.DeviceSummary
import pl.sekula.myvideofonlite.domain.model.Outcome
import pl.sekula.myvideofonlite.domain.model.ThemeMode
import pl.sekula.myvideofonlite.domain.model.defaultMessage
import pl.sekula.myvideofonlite.domain.repository.DoorbellRepository
import pl.sekula.myvideofonlite.domain.repository.SettingsRepository
import pl.sekula.myvideofonlite.domain.usecase.ConnectToDoorbell

class SettingsViewModel(
    private val repository: DoorbellRepository,
    private val settingsRepository: SettingsRepository,
    private val demo: DemoBackend,
    private val connectToDoorbell: ConnectToDoorbell,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val authState: StateFlow<AuthState> = repository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.SignedOut)

    val demoOffline: StateFlow<Boolean> = demo.simulateOffline

    private val _devices = MutableStateFlow<List<DeviceSummary>>(emptyList())
    val devices: StateFlow<List<DeviceSummary>> = _devices.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadDevices()
    }

    fun setMode(mode: BackendMode) = launchBusy {
        if (settings.value.backendMode == mode) return@launchBusy
        settingsRepository.setBackendMode(mode)
        settingsRepository.setSelectedDevice(null)
        _devices.value = emptyList()
        loadDevicesNow()
    }

    fun loadDevices() = launchBusy { loadDevicesNow() }

    private suspend fun loadDevicesNow() {
        when (val r = repository.listDevices()) {
            is Outcome.Success -> _devices.value = r.value
            is Outcome.Failure -> {
                _devices.value = emptyList()
                _message.value = r.message ?: r.error.defaultMessage()
            }
        }
    }

    fun selectDevice(id: String) = launchBusy {
        settingsRepository.setSelectedDevice(id)
        when (val r = connectToDoorbell()) {
            is Outcome.Success -> _message.value = "Wybrano: ${r.value.name}"
            is Outcome.Failure -> _message.value = r.message ?: r.error.defaultMessage()
        }
    }

    fun signIn(countryCode: String, email: String, password: String) = launchBusy {
        _message.value = when (val r = repository.signIn(countryCode, email, password)) {
            is Outcome.Success -> "Zalogowano."
            is Outcome.Failure -> r.message ?: r.error.defaultMessage()
        }
        if (authState.value is AuthState.SignedIn) loadDevicesNow()
    }

    /** Okno z wynikiem operacji na koncie (wysłanie kodu, rejestracja). */
    private val _dialog = MutableStateFlow<String?>(null)
    val dialog: StateFlow<String?> = _dialog.asStateFlow()

    fun dialogShown() {
        _dialog.value = null
    }

    fun requestRegistrationCode(countryCode: String, email: String) = launchBusy {
        _dialog.value = when (val r = repository.requestRegistrationCode(countryCode, email)) {
            is Outcome.Success -> "Tuya przyjęła prośbę i wysyła kod na $email. Mail może przyjść po kilku minutach – sprawdź też Spam i Oferty."
            is Outcome.Failure -> "Nie wysłano kodu.\n\n" + (r.message ?: r.error.defaultMessage())
        }
    }

    fun register(countryCode: String, email: String, password: String, code: String) = launchBusy {
        _dialog.value = when (val r = repository.register(countryCode, email, password, code)) {
            is Outcome.Success -> "Konto założone i zalogowano. Teraz w sekcji „Urządzenie” kliknij „Dodaj domofon”."
            is Outcome.Failure -> "Nie udało się założyć konta.\n\n" + (r.message ?: r.error.defaultMessage())
        }
        if (authState.value is AuthState.SignedIn) loadDevicesNow()
    }

    fun signOut() = launchBusy {
        repository.signOut()
        _devices.value = emptyList()
    }

    fun setRequireBiometric(required: Boolean) {
        viewModelScope.launch { settingsRepository.setRequireBiometricForGate(required) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun simulateRing() {
        demo.simulateRing()
        _message.value = "Wysłano symulowane zdarzenie dzwonka."
    }

    fun setDemoOffline(offline: Boolean) = launchBusy {
        demo.simulateOffline.value = offline
        connectToDoorbell()
    }

    fun messageShown() {
        _message.value = null
    }

    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }
}
