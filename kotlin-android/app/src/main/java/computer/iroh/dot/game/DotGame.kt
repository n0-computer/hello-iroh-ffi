package computer.iroh.dot.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max
import kotlin.math.min

data class Vec2(val x: Float, val y: Float) {
    companion object {
        val ZERO = Vec2(0f, 0f)
    }
}

class DotGame {
    var myPos: Vec2 by mutableStateOf(Vec2.ZERO)
        private set
    var theirPos: Vec2 by mutableStateOf(Vec2.ZERO)
        private set

    fun setMyPos(x: Float, y: Float) {
        myPos = Vec2(clamp(x), clamp(y))
    }

    fun receivedTheirPos(x: Float, y: Float) {
        theirPos = Vec2(clamp(x), clamp(y))
    }

    /** Recenter the peer's dot for a new session. The local dot follows motion. */
    fun resetForNewSession() {
        theirPos = Vec2.ZERO
    }

    fun sessionEnded() {
        theirPos = Vec2.ZERO
    }

    companion object {
        const val DOT_RADIUS = 0.045f
    }
}

private fun clamp(v: Float, lo: Float = -1f, hi: Float = 1f): Float = max(lo, min(hi, v))
