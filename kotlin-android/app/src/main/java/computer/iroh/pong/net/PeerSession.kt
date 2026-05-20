package computer.iroh.pong.net

import computer.iroh.BiStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tagged-frame loop on a single bi-directional stream. Both peers run the
 * same send/recv loops; the producer callback decides per-tick what to
 * send, including whether ball frames go out (authority-only).
 */
class PeerSession(
    private val bi: BiStream,
    private val produceFrames: suspend () -> Pair<ByteArray, ByteArray?>,
    private val onPaddleReceived: (Float) -> Unit,
    private val onBallReceived: (WireFormat.BallPayload) -> Unit,
    private val onClosed: () -> Unit,
) {
    private var sendJob: Job? = null
    private var recvJob: Job? = null

    fun start(scope: CoroutineScope) {
        sendJob = scope.launch { runSendLoop() }
        recvJob = scope.launch { runRecvLoop() }
    }

    fun stop() {
        sendJob?.cancel()
        recvJob?.cancel()
    }

    private suspend fun runSendLoop() {
        val send = bi.send()
        val tickMs = 16L
        try {
            while (currentCoroutineContext().isActive) {
                val (paddle, ball) = produceFrames()
                send.writeAll(paddle)
                if (ball != null) send.writeAll(ball)
                delay(tickMs)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Stream closed or write failed; fall through to onClosed.
        } finally {
            onClosed()
        }
    }

    private suspend fun runRecvLoop() {
        val recv = bi.recv()
        try {
            while (currentCoroutineContext().isActive) {
                val tagBytes = recv.readExact(1u)
                when (tagBytes[0]) {
                    WireFormat.TAG_PADDLE -> {
                        val body = recv.readExact(WireFormat.PADDLE_FRAME_SIZE - 1u)
                        WireFormat.decodePaddleBody(body)?.let(onPaddleReceived)
                    }
                    WireFormat.TAG_BALL -> {
                        val body = recv.readExact(WireFormat.BALL_FRAME_SIZE - 1u)
                        WireFormat.decodeBallBody(body)?.let(onBallReceived)
                    }
                    else -> return
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Stream closed or read errored; fall through to onClosed.
        } finally {
            onClosed()
        }
    }
}
