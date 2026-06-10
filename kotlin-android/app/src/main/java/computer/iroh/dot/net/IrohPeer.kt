package computer.iroh.dot.net

import android.os.Build
import android.util.Log
import computer.iroh.BiStream
import computer.iroh.Endpoint
import computer.iroh.EndpointAddr
import computer.iroh.EndpointId
import computer.iroh.EndpointOptions
import computer.iroh.ServicesClient
import computer.iroh.ServicesOptions
import computer.iroh.presetN0
import computer.iroh.dot.game.DotGame
import computer.iroh.dot.game.MotionSource
import computer.iroh.dot.identity.IdentityStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "IrohPeer"

class IrohPeer(
    private val scope: CoroutineScope,
    private val motion: MotionSource,
    private val identity: IdentityStore,
) {
    sealed interface State {
        data object Idle : State
        data object Binding : State
        data object Ready : State
        data object Connecting : State
        data class Connected(val peerShortId: String) : State
        data class Error(val message: String) : State
    }

    val game = DotGame()

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _endpointId = MutableStateFlow(identity.endpointId)
    val endpointId: StateFlow<String> = _endpointId.asStateFlow()

    private var endpoint: Endpoint? = null
    private var acceptJob: Job? = null
    private var session: PeerSession? = null
    private var services: ServicesClient? = null

    fun start() {
        if (endpoint != null) return
        _state.value = State.Binding
        scope.launch {
            try {
                val ep = Endpoint.bind(
                    EndpointOptions(
                        preset = presetN0(),
                        secretKey = identity.secretKey.toBytes(),
                        alpns = listOf(WireFormat.ALPN),
                    ),
                )
                endpoint = ep
                _state.value = State.Ready
                acceptJob = scope.launch { runAcceptLoop(ep) }
                startServicesClient()
            } catch (t: Throwable) {
                _state.value = State.Error("bind failed: ${t.message ?: t}")
            }
        }
    }

    fun connect(toEndpointIdHex: String) {
        val ep = endpoint ?: return
        val trimmed = toEndpointIdHex.trim()
        scope.launch {
            val parsed = try {
                EndpointId.fromString(trimmed)
            } catch (t: Throwable) {
                _state.value = State.Error("invalid endpoint id")
                return@launch
            }
            _state.value = State.Connecting
            try {
                val addr = EndpointAddr(parsed, null, emptyList())
                val conn = ep.connect(addr, WireFormat.ALPN)
                val bi = conn.openBi()
                adoptSession(bi, parsed.toString())
            } catch (t: Throwable) {
                _state.value = State.Error("connect failed: ${t.message ?: t}")
            }
        }
    }

    private suspend fun startServicesClient() {
        val ep = endpoint ?: return
        try {
            services = ServicesClient.create(
                ep,
                ServicesOptions(apiSecret = API_SECRET, name = deviceName()),
            )
        } catch (t: Throwable) {
            Log.w(TAG, "iroh services client not started: ${t.message ?: t}")
        }
    }

    private fun deviceName(): String {
        val short = _endpointId.value.take(8)
        val model = Build.MODEL.lowercase().replace(" ", "-")
        return "android-$model-$short"
    }

    private suspend fun runAcceptLoop(ep: Endpoint) {
        while (currentCoroutineContext().isActive) {
            val incoming = try {
                ep.acceptNext() ?: return
            } catch (t: Throwable) {
                Log.w(TAG, "acceptNext threw", t)
                continue
            }
            try {
                val accepting = incoming.accept()
                val alpn = accepting.alpn()
                if (!alpn.contentEquals(WireFormat.ALPN)) continue
                val conn = accepting.connect()
                val bi = conn.acceptBi()
                val remoteIdHex = conn.remoteId().toString()
                adoptSession(bi, remoteIdHex)
            } catch (t: Throwable) {
                Log.w(TAG, "accept flow threw", t)
            }
        }
    }

    private fun adoptSession(bi: BiStream, remoteIdHex: String) {
        session?.stop()
        game.resetForNewSession()
        val g = game
        val m = motion
        lateinit var s: PeerSession
        s = PeerSession(
            bi = bi,
            produceFrame = {
                g.setMyPos(m.x, m.y)
                WireFormat.encodePosition(g.myPos.x, g.myPos.y)
            },
            onPositionReceived = { p -> g.receivedTheirPos(p.x, p.y) },
            // Capture the session identity in the closure so a late-firing
            // onClosed from a previous session can't tear down the current one.
            onClosed = { if (session === s) handleSessionClosed() },
        )
        session = s
        s.start(scope)
        val short = remoteIdHex.take(10)
        _state.value = State.Connected(short)
        Log.d(TAG, "session adopted with $remoteIdHex")
    }

    private fun handleSessionClosed() {
        session = null
        game.sessionEnded()
        if (_state.value is State.Connected) _state.value = State.Ready
    }

    companion object {
        // Paste an API key from https://services.iroh.computer to see this
        // device's metrics in your dashboard. With the placeholder left in
        // place the services client fails to start; the demo works either way.
        const val API_SECRET = "<your-iroh-services-api-key>"
    }
}
