package pl.sekula.myvideofonlite.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import pl.sekula.myvideofonlite.R
import pl.sekula.myvideofonlite.domain.model.Capability
import pl.sekula.myvideofonlite.domain.model.DoorbellDevice
import pl.sekula.myvideofonlite.domain.usecase.HomeState
import pl.sekula.myvideofonlite.presentation.common.AppTopBar
import pl.sekula.myvideofonlite.presentation.common.GateConfirmationDialog
import pl.sekula.myvideofonlite.presentation.common.IconBadge
import pl.sekula.myvideofonlite.presentation.common.SectionCard
import pl.sekula.myvideofonlite.presentation.common.StatusPill
import pl.sekula.myvideofonlite.presentation.common.formatDateTime
import pl.sekula.myvideofonlite.presentation.theme.HeroGradient
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(title = "MyVideoFon") {
                IconButton(onClick = onOpenEvents) {
                    Icon(painterResource(R.drawable.ic_history), contentDescription = "Historia zdarzeń")
                }
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val s = state
            if (s == null) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            when (s.kind) {
                HomeState.Kind.NOT_CONFIGURED -> HeroInfo(
                    title = "Nie skonfigurowano urządzenia",
                    body = "1. Otwórz ustawienia.\n" +
                        "2. Wybierz źródło danych: „Demo” (symulator) lub „Tuya”.\n" +
                        "3. Pobierz listę urządzeń i wybierz swój wideodomofon.",
                    action = "Przejdź do ustawień",
                    onAction = onOpenSettings,
                )
                HomeState.Kind.AUTH_ERROR -> HeroInfo(
                    title = "Błąd autoryzacji",
                    body = "Sesja wygasła lub dane logowania są nieprawidłowe.",
                    action = "Zaloguj ponownie",
                    onAction = onOpenSettings,
                )
                else -> DeviceHero(s, refreshing)
            }
            if (s.kind != HomeState.Kind.NOT_CONFIGURED) {
                Button(
                    onClick = onOpenLiveView,
                    enabled = s.canStartLiveView,
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_videocam), contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Podgląd na żywo", style = MaterialTheme.typography.titleMedium)
                }
                if (s.showGateButton) {
                    FilledTonalButton(
                        onClick = { showGateDialog = true },
                        enabled = s.canOpenGate,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_lock_open), contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Otwórz furtkę", style = MaterialTheme.typography.titleMedium)
                    }
                }
                s.device?.let { CapabilitiesCard(it) }
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
private fun DeviceHero(state: HomeState, refreshing: Boolean) {
    val device = state.device ?: return
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(HeroGradient)
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(R.drawable.ic_doorbell, container = Color.White.copy(alpha = 0.15f), content = Color.White, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "WIDEODOMOFON",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Text(device.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
                if (refreshing) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            }
            val (color, label) = when (state.kind) {
                HomeState.Kind.ONLINE -> StatusOnline to "Online"
                HomeState.Kind.OFFLINE -> StatusOffline to "Offline"
                else -> StatusUnknown to "Stan nieznany"
            }
            StatusPill(label, color, onDark = true)
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_history),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ostatni dzwonek: " + (state.lastRing?.let { formatDateTime(it.timestampMillis) } ?: "brak"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            when (state.kind) {
                HomeState.Kind.OFFLINE -> Text("Brak połączenia z urządzeniem.", color = Color(0xFFFCA5A5))
                HomeState.Kind.UNKNOWN -> Text(
                    "Nie udało się potwierdzić stanu urządzenia. Odśwież, aby spróbować ponownie.",
                    color = Color.White.copy(alpha = 0.85f),
                )
                else -> Unit
            }
            state.error?.let {
                Text("Błąd połączenia: $it", color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CapabilitiesCard(device: DoorbellDevice) {
    SectionCard(title = "Funkcje urządzenia") {
        val items = listOf(
            Triple(R.drawable.ic_videocam, "Obraz na żywo", Capability.LIVE_VIDEO),
            Triple(R.drawable.ic_mic, "Rozmowa", Capability.TWO_WAY_AUDIO),
            Triple(R.drawable.ic_lock_open, "Furtka", Capability.GATE_UNLOCK),
            Triple(R.drawable.ic_doorbell, "Dzwonek", Capability.DOORBELL_EVENTS),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            items.forEach { (icon, label, cap) ->
                val on = device.supports(cap)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    IconBadge(
                        icon,
                        container = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        content = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 44.dp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroInfo(title: String, body: String, action: String, onAction: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(HeroGradient)
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            IconBadge(R.drawable.ic_doorbell, container = Color.White.copy(alpha = 0.15f), content = Color.White, size = 56.dp)
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A)),
            ) { Text(action) }
        }
    }
}
