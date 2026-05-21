package computer.iroh.pong.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import computer.iroh.pong.MainViewModel
import computer.iroh.pong.game.PongColors
import computer.iroh.pong.game.PongScene
import computer.iroh.pong.net.IrohPeer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndpointScreen(viewModel: MainViewModel) {
    val state by viewModel.peer.state.collectAsState()
    val endpointId by viewModel.peer.endpointId.collectAsState()
    val context = LocalContext.current
    var peerIdInput by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("iroh-pong") },
                actions = {
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Your endpoint id", style = MaterialTheme.typography.labelSmall)
            SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = endpointId,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 2,
                )
            }
            Button(
                onClick = { copyToClipboard(context, endpointId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = endpointId.isNotBlank(),
            ) {
                Text("Copy endpoint id")
            }

            Spacer(modifier = Modifier.height(2.dp))

            OutlinedTextField(
                value = peerIdInput,
                onValueChange = { peerIdInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Peer endpoint id") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.peer.connect(peerIdInput) },
                    enabled = state is IrohPeer.State.Ready && peerIdInput.isNotBlank(),
                ) {
                    Text("Connect")
                }
                Text(
                    text = stateLabel(state),
                    color = stateColor(state),
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            val myColor = PongColors.forEndpointId(endpointId)
            val opponentColor = PongColors.forEndpointId(
                when (val s = state) {
                    is IrohPeer.State.Connected -> s.peerShortId
                    else -> peerIdInput
                },
            )

            PongScene(
                game = viewModel.peer.game,
                myColor = myColor,
                opponentColor = opponentColor,
                onDrag = viewModel.motion::setFromDrag,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }

        if (settingsOpen) {
            SettingsBottomSheet(
                peer = viewModel.peer,
                onDismiss = { settingsOpen = false },
            )
        }
    }
}

@Composable
private fun stateColor(state: IrohPeer.State) = when (state) {
    is IrohPeer.State.Error -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun stateLabel(state: IrohPeer.State): String = when (state) {
    IrohPeer.State.Idle -> "Idle"
    IrohPeer.State.Binding -> "Binding…"
    IrohPeer.State.Ready -> "Ready"
    IrohPeer.State.Connecting -> "Connecting…"
    is IrohPeer.State.Connected -> "Connected to ${state.peerShortId}"
    is IrohPeer.State.Error -> "Error: ${state.message}"
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("endpoint id", text))
    Toast.makeText(context, "Endpoint id copied", Toast.LENGTH_SHORT).show()
}
