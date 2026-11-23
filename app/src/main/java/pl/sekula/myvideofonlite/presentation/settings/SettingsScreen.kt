package pl.sekula.myvideofonlite.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import pl.sekula.myvideofonlite.R
import pl.sekula.myvideofonlite.presentation.common.SectionCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import pl.sekula.myvideofonlite.domain.model.AuthState
import pl.sekula.myvideofonlite.domain.model.BackendMode
import pl.sekula.myvideofonlite.presentation.common.AppTopBar

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val auth by viewModel.authState.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val demoOffline by viewModel.demoOffline.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Ustawienia", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            SectionCard("Źródło danych") {
                RadioRow("Demo (symulowany wideodomofon)", settings.backendMode == BackendMode.DEMO) {
                    viewModel.setMode(BackendMode.DEMO)
                }
                RadioRow("Tuya (SmartLife App SDK + IPC SDK)", settings.backendMode == BackendMode.TUYA) {
                    viewModel.setMode(BackendMode.TUYA)
                }
            }

            if (settings.backendMode == BackendMode.TUYA) {
                SectionCard("Konto Tuya") {
                    TuyaAccountSection(
                        auth = auth,
                        onSignIn = viewModel::signIn,
                        onSignOut = viewModel::signOut,
                    )
                }
            }

            SectionCard("Urządzenie") {
                if (devices.isEmpty()) {
                    Text(
                        "Brak urządzeń na liście.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                devices.forEach { d ->
                    RadioRow(d.name, settings.selectedDeviceId == d.id) { viewModel.selectDevice(d.id) }
                }
                OutlinedButton(onClick = viewModel::loadDevices) { Text("Pobierz listę urządzeń") }
            }

            SectionCard("Bezpieczeństwo") {
                SwitchRow(
                    label = "Wymagaj odcisku palca / PIN-u przy otwieraniu furtki",
                    checked = settings.requireBiometricForGate,
                    onChange = viewModel::setRequireBiometric,
                )
            }

            if (settings.backendMode == BackendMode.DEMO) {
                SectionCard("Narzędzia demo") {
                    Button(onClick = viewModel::simulateRing) {
                        Icon(painterResource(R.drawable.ic_doorbell), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Symuluj dzwonek")
                    }
                    SwitchRow(
                        label = "Symuluj brak połączenia z urządzeniem",
                        checked = demoOffline,
                        onChange = viewModel::setDemoOffline,
                    )
                }
            }
        }
    }
}

@Composable
private fun TuyaAccountSection(
    auth: AuthState,
    onSignIn: (String, String, String) -> Unit,
    onSignOut: () -> Unit,
) {
    var country by rememberSaveable { mutableStateOf("48") }
    var email by rememberSaveable { mutableStateOf("") }
    // Hasło nie jest zapisywane ani logowane.
    var password by remember { mutableStateOf("") }

    Text(
        "Integracja Tuya wymaga wcześniejszej weryfikacji urządzenia i kluczy SDK (Etap 0). " +
            "Szczegóły: docs/TUYA_INTEGRATION.md w repozytorium.",
        style = MaterialTheme.typography.bodySmall,
    )
    when (auth) {
        is AuthState.SignedIn -> {
            Text("Zalogowano: ${auth.accountLabel}")
            OutlinedButton(onClick = onSignOut) { Text("Wyloguj") }
        }
        else -> {
            if (auth is AuthState.Expired) {
                Text("Sesja wygasła. Zaloguj się ponownie.", color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(
                value = country,
                onValueChange = { country = it.filter(Char::isDigit) },
                label = { Text("Kod kraju (np. 48)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("E-mail konta Smart Life") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Hasło") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onSignIn(country, email, password)
                    password = ""
                },
                enabled = email.isNotBlank() && password.isNotBlank(),
            ) { Text("Zaloguj") }
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
