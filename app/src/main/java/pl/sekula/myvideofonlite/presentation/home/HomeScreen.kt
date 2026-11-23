package pl.sekula.myvideofonlite.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import pl.sekula.myvideofonlite.domain.usecase.HomeState
import pl.sekula.myvideofonlite.presentation.common.AppTopBar
import pl.sekula.myvideofonlite.presentation.common.GateConfirmationDialog
import pl.sekula.myvideofonlite.presentation.common.formatDateTime
import pl.sekula.myvideofonlite.presentation.theme.StatusOffline
import pl.sekula.myvideofonlite.presentation.theme.StatusOnline
import pl.sekula.myvideofonlite.presentation.theme.StatusUnknown

@Composable
fun HomeScreen(
    gateRequested: Boolean,
    onGateRequestHandled: () -> Unit,
    onOpenLiveView: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEvents: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val requireBiometric by viewModel.requireBiometric.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showGateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }
    // Żądanie z widgetu: pokazujemy okno potwierdzenia, nigdy nie otwieramy od razu.
    LaunchedEffect(gateRequested, state) {
        val s = state ?: return@LaunchedEffect
        if (gateRequested) {
            if (s.canOpenGate) showGateDialog = true
            else viewModel.showMessage("Otwieranie furtki jest teraz niedostępne.")
            onGateRequestHandled()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "MyVideoFon Lite") {
                IconButton(onClick = onOpenEvents) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Historia zdarzeń") }
                IconButton(onClick = viewModel::refresh) { Icon(Icons.Filled.Refresh, contentDescription = "Odśwież") }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Ustawienia") }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val s = state
            if (s == null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            when (s.kind) {
                HomeState.Kind.NOT_CONFIGURED -> NotConfiguredCard(onOpenSettings)
                HomeState.Kind.AUTH_ERROR -> InfoCard(
                    title = "Błąd autoryzacji",
                    body = "Sesja wygasła lub dane logowania są nieprawidłowe.",
                    action = "Zaloguj ponownie",
                    onAction = onOpenSettings,
                )
                else -> DeviceCard(s, refreshing)
            }
            if (s.kind != HomeState.Kind.NOT_CONFIGURED) {
                Button(
                    onClick = onOpenLiveView,
                    enabled = s.canStartLiveView,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Podgląd na żywo") }
                if (s.showGateButton) {
                    OutlinedButton(
                        onClick = { showGateDialog = true },
                        enabled = s.canOpenGate,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) { Text("Otwórz furtkę") }
                }
            }
        }
    }

    val device = state?.device
    if (showGateDialog && device != null) {
        GateConfirmationDialog(
            deviceName = device.name,
            requireBiometric = requireBiometric,
            onDismiss = { showGateDialog = false },
            onConfirmed = viewModel::openGateConfirmed,
            onAuthError = { viewModel.showMessage("Nie potwierdzono: $it") },
        )
    }
}

@Composable
private fun DeviceCard(state: HomeState, refreshing: Boolean) {
    val device = state.device ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (color, label) = when (state.kind) {
                    HomeState.Kind.ONLINE -> StatusOnline to "Online"
                    HomeState.Kind.OFFLINE -> StatusOffline to "Offline"
                    else -> StatusUnknown to "Stan nieznany"
                }
                Box(Modifier.size(12.dp).background(color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (refreshing) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            Text(
                "Ostatni dzwonek: " + (state.lastRing?.let { formatDateTime(it.timestampMillis) } ?: "brak"),
                style = MaterialTheme.typography.bodyMedium,
            )
            when (state.kind) {
                HomeState.Kind.OFFLINE -> Text("Brak połączenia z urządzeniem.", color = MaterialTheme.colorScheme.error)
                HomeState.Kind.UNKNOWN -> Text("Nie udało się potwierdzić stanu urządzenia. Odśwież, aby spróbować ponownie.")
                else -> Unit
            }
            state.error?.let {
                Text("Błąd połączenia: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NotConfiguredCard(onOpenSettings: () -> Unit) {
    InfoCard(
        title = "Nie skonfigurowano urządzenia",
        body = "1. Otwórz ustawienia.\n" +
            "2. Wybierz źródło danych: „Demo” (symulator) lub „Tuya” (po weryfikacji urządzenia).\n" +
            "3. Pobierz listę urządzeń i wybierz swój wideodomofon.",
        action = "Przejdź do ustawień",
        onAction = onOpenSettings,
    )
}

@Composable
private fun InfoCard(title: String, body: String, action: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body)
            Button(onClick = onAction) { Text(action) }
        }
    }
}

