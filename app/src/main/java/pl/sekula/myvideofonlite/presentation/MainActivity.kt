package pl.sekula.myvideofonlite.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import pl.sekula.myvideofonlite.presentation.theme.MyVideoFonTheme

/** FragmentActivity – wymagana przez BiometricPrompt; Compose działa bez zmian. */
class MainActivity : FragmentActivity() {

    private val pendingAction = MutableStateFlow<String?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* wynik widoczny w ustawieniach */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingAction.value = intent?.getStringExtra(EXTRA_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MyVideoFonTheme {
                AppNavHost(
                    pendingAction = pendingAction,
                    onActionConsumed = { pendingAction.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAction.value = intent.getStringExtra(EXTRA_ACTION)
    }

    companion object {
        const val EXTRA_ACTION = "action"
        const val ACTION_LIVE_VIEW = "live_view"
        const val ACTION_GATE = "gate"

        fun intent(context: Context, action: String?): Intent =
            Intent(context, MainActivity::class.java).apply {
                if (action != null) putExtra(EXTRA_ACTION, action)
                // Różne akcje = różne PendingIntenty (widget).
                data = android.net.Uri.parse("myvideofon://open/${action ?: "home"}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}
