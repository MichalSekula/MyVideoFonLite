package pl.sekula.myvideofonlite.presentation.liveview

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import pl.sekula.myvideofonlite.R
import pl.sekula.myvideofonlite.presentation.common.RoundAction
import pl.sekula.myvideofonlite.presentation.common.StatusPill
import pl.sekula.myvideofonlite.presentation.theme.StatusOffline
import pl.sekula.myvideofonlite.presentation.theme.StatusOnline
import pl.sekula.myvideofonlite.presentation.theme.StatusUnknown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { fullscreen = false }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_fullscreen_exit), contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Zamknij pełny ekran", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = device?.name ?: "Podgląd", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.large),
            ) {
                video(Modifier.fillMaxSize())
                if (state is LiveViewState.Connected) {
                    Row(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(6.dp).background(Color.White, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("NA ŻYWO", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
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
        LiveViewState.Idle, LiveViewState.Connecting -> "Łączenie…" to Color(0xFFF59E0B)
        is LiveViewState.Connected -> "Połączono" to StatusOnline
        LiveViewState.Disconnected -> "Rozłączono" to StatusUnknown
        is LiveViewState.Error -> "Błąd połączenia" to StatusOffline
    }
    StatusPill(label, color)
}

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
    val scheme = MaterialTheme.colorScheme
    val neutral = scheme.surfaceContainerHigh
    val actions = buildList<@Composable (Modifier) -> Unit> {
        if (audioSupported) {
            add { m ->
                val on = connected?.audioEnabled == true
                RoundAction(
                    label = if (on) "Wycisz" else "Włącz dźwięk",
                    icon = if (on) R.drawable.ic_volume_up else R.drawable.ic_volume_off,
                    container = if (on) scheme.primary else neutral,
                    content = if (on) scheme.onPrimary else scheme.onSurface,
                    enabled = connected != null,
                    modifier = m,
                    onClick = { onAudio(!on) },
                )
            }
            add { m ->
                val talking = connected?.talking == true
                RoundAction(
                    label = if (talking) "Zakończ rozmowę" else "Rozpocznij rozmowę",
                    icon = if (talking) R.drawable.ic_call_end else R.drawable.ic_mic,
                    container = if (talking) Color(0xFFDC2626) else scheme.primary,
                    content = if (talking) Color.White else scheme.onPrimary,
                    enabled = connected != null,
                    modifier = m,
                    onClick = { onTalk(!talking) },
                )
            }
        }
        if (gateSupported) {
            add { m ->
                RoundAction(
                    label = "Otwórz furtkę",
                    icon = R.drawable.ic_lock_open,
                    container = scheme.secondary,
                    content = scheme.onSecondary,
                    enabled = gateEnabled,
                    modifier = m,
                    onClick = onGate,
                )
            }
        }
        add { m ->
            RoundAction(
                label = "Pełny ekran",
                icon = R.drawable.ic_fullscreen,
                container = neutral,
                content = scheme.onSurface,
                enabled = connected != null,
                modifier = m,
                onClick = onFullscreen,
            )
        }
        add { m ->
            RoundAction(
                label = "Połącz ponownie",
                vector = Icons.Filled.Refresh,
                container = neutral,
                content = scheme.onSurface,
                modifier = m,
                onClick = onReconnect,
            )
        }
        if (connected != null) {
            add { m ->
                RoundAction(
                    label = "Rozłącz",
                    vector = Icons.Filled.Close,
                    container = neutral,
                    content = scheme.onSurface,
                    modifier = m,
                    onClick = onDisconnect,
                )
            }
        }
    }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            actions.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { action -> action(Modifier.weight(1f)) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    if (!audioSupported) {
        Text(
            "Urządzenie nie udostępnia rozmowy dwukierunkowej.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
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
