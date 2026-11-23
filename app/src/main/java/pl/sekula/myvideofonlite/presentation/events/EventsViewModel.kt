package pl.sekula.myvideofonlite.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import pl.sekula.myvideofonlite.domain.model.DoorbellEvent
import pl.sekula.myvideofonlite.domain.repository.DoorbellRepository

class EventsViewModel(repository: DoorbellRepository) : ViewModel() {
    val events: StateFlow<List<DoorbellEvent>> = repository.events
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
