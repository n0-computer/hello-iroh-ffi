package computer.iroh.pong.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import computer.iroh.pong.net.IrohPeer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    peer: IrohPeer,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val apiSecret by peer.apiSecret.collectAsState()
    val telemetry by peer.telemetry.collectAsState()
    var input by remember(apiSecret) { mutableStateOf(apiSecret) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.titleMedium)
            Text("iroh services API key", style = MaterialTheme.typography.labelLarge)

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("services1…") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            val trimmed = input.trim()
            val saveEnabled = trimmed.isNotEmpty() && trimmed != apiSecret
            val clearEnabled = apiSecret.isNotEmpty() || input.isNotEmpty()

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { peer.saveApiSecret(trimmed) },
                    enabled = saveEnabled,
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = {
                        input = ""
                        peer.saveApiSecret("")
                    },
                    enabled = clearEnabled,
                ) {
                    Text("Clear")
                }
            }

            Text(
                text = if (peer.isUsingDefaultApiSecret) {
                    "Using bundled default key. Paste a secret from services.iroh.computer to override; stored locally on this device only."
                } else {
                    "Using a custom key. Tap Clear to revert to the bundled default."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Telemetry status", style = MaterialTheme.typography.labelLarge)
            Text(
                text = telemetryLine(telemetry),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

private fun telemetryLine(state: IrohPeer.TelemetryState): String = when (state) {
    IrohPeer.TelemetryState.Off -> "off — paste an API secret to enable"
    IrohPeer.TelemetryState.Starting -> "connecting…"
    is IrohPeer.TelemetryState.Active -> "active — pushing as ${state.name}"
    is IrohPeer.TelemetryState.Error -> "error: ${state.message}"
}
