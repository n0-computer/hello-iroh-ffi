package computer.iroh.pong.net

import android.util.Log
import computer.iroh.BiStream
import computer.iroh.Endpoint
import computer.iroh.EndpointAddr
import computer.iroh.EndpointId
import computer.iroh.EndpointOptions
import computer.iroh.presetN0
import computer.iroh.pong.game.MotionSource
import computer.iroh.pong.game.PongGame
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
) {
    sealed interface State {
        data object Idle : State
        data object Binding : State
        data object Ready : State
        data object Connecting : State
        data class Connected(val peerShortId: String) : State
        data class Error(val message: String) : State
    }

    val game = PongGame()

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _endpointId = MutableStateFlow<String?>(null)
    val endpointId: StateFlow<String?> = _endpointId.asStateFlow()

    private var endpoint: Endpoint? = null
    private var acceptJob: Job? = null
    private var session: PeerSession? = null

    fun start() {
        if (endpoint != null) return
        _state.value = State.Binding
        scope.launch {
            try {
                val ep = Endpoint.bind(
                    EndpointOptions(
                        preset = presetN0(),
                        alpns = listOf(WireFormat.ALPN),
                    ),
                )
                endpoint = ep
                _endpointId.value = ep.id().toString()
                _state.value = State.Ready
                acceptJob = scope.launch { runAcceptLoop(ep) }
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
                adoptSession(bi, parsed.toString(), asAuthority = true)
            } catch (t: Throwable) {
                _state.value = State.Error("connect failed: ${t.message ?: t}")
            }
        }
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
                adoptSession(bi, remoteIdHex, asAuthority = false)
            } catch (t: Throwable) {
                Log.w(TAG, "accept flow threw", t)
            }
        }
    }

    private fun adoptSession(bi: BiStream, remoteIdHex: String, asAuthority: Boolean) {
        session?.stop()
        game.resetForNewSession(asAuthority)
        val g = game
        val m = motion
        val s = PeerSession(
            bi = bi,
            produceFrames = { g.produceTickFrames(m.paddleX) },
            onPaddleReceived = { x -> g.receivedOpponentPaddle(x) },
            onBallReceived = { p -> g.receivedBall(p) },
            onClosed = ::handleSessionClosed,
        )
        session = s
        s.start(scope)
        val short = remoteIdHex.take(10)
        _state.value = State.Connected(short)
        Log.d(TAG, "session adopted with $remoteIdHex (authority=$asAuthority)")
    }

    private fun handleSessionClosed() {
        if (session == null) return
        session = null
        game.sessionEnded()
        if (_state.value is State.Connected) _state.value = State.Ready
    }
}
