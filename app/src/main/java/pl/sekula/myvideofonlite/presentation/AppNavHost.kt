package pl.sekula.myvideofonlite.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.StateFlow
import pl.sekula.myvideofonlite.presentation.events.EventsScreen
import pl.sekula.myvideofonlite.presentation.home.HomeScreen
import pl.sekula.myvideofonlite.presentation.liveview.LiveViewScreen
import pl.sekula.myvideofonlite.presentation.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val LIVE_VIEW = "liveview"
    const val SETTINGS = "settings"
    const val EVENTS = "events"
}

@Composable
fun AppNavHost(pendingAction: StateFlow<String?>, onActionConsumed: () -> Unit) {
    val nav = rememberNavController()
    val action by pendingAction.collectAsStateWithLifecycle()
    var gateRequested by remember { mutableStateOf(false) }

    LaunchedEffect(action) {
        when (action) {
            MainActivity.ACTION_LIVE_VIEW -> {
                nav.navigate(Routes.LIVE_VIEW) {
                    popUpTo(Routes.HOME)
                    launchSingleTop = true
                }
                onActionConsumed()
            }
            MainActivity.ACTION_GATE -> {
                nav.popBackStack(Routes.HOME, inclusive = false)
                gateRequested = true
                onActionConsumed()
            }
        }
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                gateRequested = gateRequested,
                onGateRequestHandled = { gateRequested = false },
                onOpenLiveView = { nav.navigate(Routes.LIVE_VIEW) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenEvents = { nav.navigate(Routes.EVENTS) },
            )
        }
        composable(Routes.LIVE_VIEW) { LiveViewScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.EVENTS) { EventsScreen(onBack = { nav.popBackStack() }) }
    }
}
