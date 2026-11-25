package pl.sekula.myvideofonlite.presentation.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import org.koin.androidx.compose.koinViewModel
import pl.sekula.myvideofonlite.data.tuya.PairingMode
import pl.sekula.myvideofonlite.presentation.common.AppTopBar
import pl.sekula.myvideofonlite.presentation.common.SectionCard

@Composable
fun PairingScreen(onDone: () -> Unit, viewModel: PairingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showWifi by rememberSaveable { mutableStateOf(false) }
    var ssid by rememberSaveable { mutableStateOf("") }
    // Hasło Wi-Fi trzymamy tylko w pamięci ekranu.
    var wifiPassword by remember { mutableStateOf("") }

    fun scan() {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { code -> code.rawValue?.let(viewModel::pairScanned) }
            .addOnFailureListener { viewModel.scanFailed("Nie udało się uruchomić skanera (wymaga Usług Google Play).") }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Dodaj domofon", onBack = onDone) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.running) LinearProgressIndicator(Modifier.fillMaxWidth())

            state.pairedName?.let { name ->
                SectionCard("Gotowe") {
                    Text("Dodano „$name” i ustawiono jako domofon w aplikacji.")
                    Button(onClick = onDone) { Text("Wróć") }
                }
            }
            state.status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            SectionCard("Kod QR na ekranie domofonu") {
                Text(
                    "Domofon musi być połączony z Wi-Fi i pokazywać kod parowania. " +
                        "Zeskanuj go telefonem.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = ::scan, enabled = !state.running) { Text("Zeskanuj kod") }
                    if (state.running) OutlinedButton(onClick = viewModel::cancel) { Text("Przerwij") }
                }
            }

            TextButton(onClick = { showWifi = !showWifi }) {
                Text(if (showWifi) "Ukryj inne sposoby" else "Inne sposoby (dioda / hotspot)")
            }
            if (showWifi) {
                SectionCard("Parowanie przez Wi-Fi") {
                    Text(
                        "Telefon musi być w tej samej sieci Wi-Fi 2,4 GHz, do której ma dołączyć domofon. " +
                            "Szybko migająca dioda: „Tryb EZ”. Wolno migająca: połącz telefon z siecią " +
                            "domofonu (SmartLife-…), wróć tu i wybierz „Tryb AP”.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = ssid,
                        onValueChange = { ssid = it },
                        label = { Text("Nazwa sieci Wi-Fi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("Hasło Wi-Fi") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.pairWifi(PairingMode.EZ, ssid, wifiPassword) },
                            enabled = !state.running && ssid.isNotBlank(),
                        ) { Text("Tryb EZ") }
                        OutlinedButton(
                            onClick = { viewModel.pairWifi(PairingMode.AP, ssid, wifiPassword) },
                            enabled = !state.running && ssid.isNotBlank(),
                        ) { Text("Tryb AP") }
                    }
                }
            }
        }
    }
}
