package pl.sekula.myvideofonlite.presentation.common

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay

/**
 * Świadome potwierdzenie otwarcia furtki (spec. rozdz. 8):
 * okno dialogowe z przyciskiem aktywnym dopiero po chwili (ochrona przed przypadkowym kliknięciem)
 * oraz – opcjonalnie – uwierzytelnienie biometryczne / PIN urządzenia.
 */
@Composable
fun GateConfirmationDialog(
    deviceName: String,
    requireBiometric: Boolean,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
    onAuthError: (String) -> Unit,
) {
    val context = LocalContext.current
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1000)
        armed = true
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Otworzyć furtkę?") },
        text = { Text("Urządzenie: $deviceName\nTa akcja otworzy wejście.") },
        confirmButton = {
            TextButton(
                enabled = armed,
                onClick = {
                    onDismiss()
                    val activity = context.findFragmentActivity()
                    if (requireBiometric && activity != null) {
                        authenticate(activity, onConfirmed, onAuthError)
                    } else {
                        onConfirmed()
                    }
                },
            ) { Text("Otwórz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

private fun authenticate(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
        // Brak skonfigurowanej blokady ekranu – pozostaje potwierdzenie w oknie dialogowym.
        onSuccess()
        return
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                onError(errString.toString())
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Potwierdź otwarcie furtki")
        .setSubtitle("Uwierzytelnij się, aby otworzyć wejście")
        .setAllowedAuthenticators(authenticators)
        .build()
    prompt.authenticate(info)
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
