package pl.sekula.myvideofonlite.presentation.liveview

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import pl.sekula.myvideofonlite.domain.model.Capability
import pl.sekula.myvideofonlite.domain.model.DeviceStatus
import pl.sekula.myvideofonlite.domain.model.LiveViewState
import pl.sekula.myvideofonlite.domain.model.defaultMessage
import pl.sekula.myvideofonlite.presentation.common.AppTopBar
import pl.sekula.myvideofonlite.presentation.common.GateConfirmationDialog
import pl.sekula.myvideofonlite.presentation.common.findFragmentActivity

@Composable
fun LiveViewScreen(onBack: () -> Unit, viewModel: LiveViewViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val device by viewModel.device.collectAsStateWithLifecycle()
    val isDemo by viewModel.isDemo.collectAsStateWithLifecycle()
    val requireBiometric by viewModel.requireBiometric.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var showGateDialog by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    FullscreenEffect(fullscreen)
    BackHandler(enabled = fullscreen) { fullscreen = false }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val video: @Composable (Modifier) -> Unit = { modifier ->
        VideoArea(
            state = state,
            isDemo = isDemo,
            onRetry = viewModel::connect,
            onAttach = viewModel::attachRenderTarget,
            modifier = modifier,
        )
    }

    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            video(Modifier.fillMaxSize())
            TextButton(
                onClick = { fullscreen = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            ) { Text("Zamknij pełny ekran", color = Color.White) }
        }
        return
    }

    Scaffold(
        topBar = { AppTopBar(title = device?.name ?: "Podgląd", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            video(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            ConnectionIndicator(state)
            Controls(
                state = state,
                audioSupported = device?.supports(Capability.TWO_WAY_AUDIO) == true,
                gateSupported = device?.supports(Capability.GATE_UNLOCK) == true,
                gateEnabled = device?.status == DeviceStatus.ONLINE,
                onAudio = viewModel::setAudio,
                onTalk = viewModel::setTalking,
                onGate = { showGateDialog = true },
                onReconnect = viewModel::connect,
                onDisconnect = viewModel::disconnect,
                onFullscreen = { fullscreen = true },
            )
        }
    }

    val d = device
    if (showGateDialog && d != null) {
        GateConfirmationDialog(
            deviceName = d.name,
            requireBiometric = requireBiometric,
            onDismiss = { showGateDialog = false },
            onConfirmed = viewModel::openGateConfirmed,
            onAuthError = { viewModel.showMessage("Nie potwierdzono: $it") },
        )
    }
}

@Composable
private fun VideoArea(
    state: LiveViewState,
    isDemo: Boolean,
    onRetry: () -> Unit,
    onAttach: (Any?) -> Unit,
    modifier: Modifier,
) {
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        when (state) {
            is LiveViewState.Connected -> if (isDemo) {
                DemoVideo(Modifier.fillMaxSize())
            } else {
                // Widok renderujący dla Tuya IPC SDK – sesja podpina do niego strumień.
                AndroidView(
                    factory = { ctx -> FrameLayout(ctx).also(onAttach) },
                    onRelease = { onAttach(null) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            LiveViewState.Idle, LiveViewState.Connecting -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Łączenie z urządzeniem…", color = Color.White)
            }
            LiveViewState.Disconnected -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rozłączono", color = Color.White)
                Button(onClick = onRetry) { Text("Połącz ponownie") }
            }
            is LiveViewState.Error -> Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message ?: state.error.defaultMessage(), color = Color.White)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRetry) { Text("Spróbuj ponownie") }
            }
        }
    }
}

@Composable
private fun ConnectionIndicator(state: LiveViewState) {
    val (label, color) = when (state) {
        LiveViewState.Idle, LiveViewState.Connecting -> "Łączenie…" to MaterialTheme.colorScheme.tertiary
        is LiveViewState.Connected -> "Połączono" to Color(0xFF2E7D32)
        LiveViewState.Disconnected -> "Rozłączono" to MaterialTheme.colorScheme.outline
        is LiveViewState.Error -> "Błąd połączenia" to MaterialTheme.colorScheme.error
    }
    Text(
        "● $label",
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Controls(
    state: LiveViewState,
    audioSupported: Boolean,
    gateSupported: Boolean,
    gateEnabled: Boolean,
    onAudio: (Boolean) -> Unit,
    onTalk: (Boolean) -> Unit,
    onGate: () -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onFullscreen: () -> Unit,
) {
    val connected = state as? LiveViewState.Connected
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (audioSupported) {
            FilledTonalButton(
                onClick = { onAudio(!(connected?.audioEnabled ?: false)) },
                enabled = connected != null,
            ) { Text(if (connected?.audioEnabled == true) "Wycisz" else "Włącz dźwięk") }
            Button(
                onClick = { onTalk(!(connected?.talking ?: false)) },
                enabled = connected != null,
            ) { Text(if (connected?.talking == true) "Zakończ rozmowę" else "Rozpocznij rozmowę") }
        }
        if (gateSupported) {
            OutlinedButton(onClick = onGate, enabled = gateEnabled) { Text("Otwórz furtkę") }
        }
        OutlinedButton(onClick = onReconnect) { Text("Połącz ponownie") }
        if (connected != null) OutlinedButton(onClick = onDisconnect) { Text("Rozłącz") }
        OutlinedButton(onClick = onFullscreen, enabled = connected != null) { Text("Pełny ekran") }
    }
    if (!audioSupported) {
        Text(
            "Urządzenie nie udostępnia rozmowy dwukierunkowej.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** Pełny ekran: ukrycie pasków systemowych i orientacja pozioma. */
@Composable
private fun FullscreenEffect(fullscreen: Boolean) {
    val activity = LocalContext.current.findFragmentActivity() as Activity?
    DisposableEffect(fullscreen, activity) {
        if (activity != null) {
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            if (fullscreen) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
        onDispose {
            if (activity != null) {
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}
