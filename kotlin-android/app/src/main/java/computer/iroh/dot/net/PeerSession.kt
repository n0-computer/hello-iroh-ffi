package computer.iroh.dot.net

import computer.iroh.BiStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Position exchange on a single bi-directional stream. Both peers run the
 * same send/recv loops: send our dot position every tick, read the peer's
 * position as it arrives. Every frame is a fixed 8 bytes, so the recv loop
 * just reads 8 bytes in a loop.
 */
class PeerSession(
    private val bi: BiStream,
    private val produceFrame: suspend () -> ByteArray,
    private val onPositionReceived: (WireFormat.Position) -> Unit,
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
                send.writeAll(produceFrame())
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
                val body = recv.readExact(WireFormat.POSITION_FRAME_SIZE)
                WireFormat.decodePosition(body)?.let(onPositionReceived)
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
