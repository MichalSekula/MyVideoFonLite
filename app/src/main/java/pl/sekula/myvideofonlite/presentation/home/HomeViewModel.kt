package pl.sekula.myvideofonlite.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.sekula.myvideofonlite.domain.model.Outcome
import pl.sekula.myvideofonlite.domain.model.defaultMessage
import pl.sekula.myvideofonlite.domain.repository.SettingsRepository
import pl.sekula.myvideofonlite.domain.usecase.ConnectToDoorbell
import pl.sekula.myvideofonlite.domain.usecase.HomeState
import pl.sekula.myvideofonlite.domain.usecase.ObserveHomeState
import pl.sekula.myvideofonlite.domain.usecase.OpenGate

class HomeViewModel(
    observeHomeState: ObserveHomeState,
    private val connectToDoorbell: ConnectToDoorbell,
    private val openGate: OpenGate,
    settings: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<HomeState?> = observeHomeState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val requireBiometric: StateFlow<Boolean> = settings.settings.map { it.requireBiometricForGate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            connectToDoorbell()
            _refreshing.value = false
        }
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
}
