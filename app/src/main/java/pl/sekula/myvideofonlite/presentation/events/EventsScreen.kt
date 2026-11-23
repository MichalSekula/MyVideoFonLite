package pl.sekula.myvideofonlite.presentation.events

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import pl.sekula.myvideofonlite.domain.model.EventType
import pl.sekula.myvideofonlite.presentation.common.AppTopBar
import pl.sekula.myvideofonlite.presentation.common.formatDateTime

@Composable
fun EventsScreen(onBack: () -> Unit, viewModel: EventsViewModel = koinViewModel()) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    Scaffold(topBar = { AppTopBar(title = "Historia zdarzeń", onBack = onBack) }) { padding ->
        if (events.isEmpty()) {
            Text("Brak zdarzeń.", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(events) { e ->
                ListItem(
                    headlineContent = {
                        Text(
                            when (e.type) {
                                EventType.RING -> "Dzwonek"
                                EventType.MOTION -> "Wykryto ruch"
                            },
                        )
                    },
                    supportingContent = { Text(formatDateTime(e.timestampMillis)) },
                )
                HorizontalDivider()
            }
        }
    }
}
