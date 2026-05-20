package computer.iroh.pong.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import computer.iroh.pong.net.WireFormat
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)

    companion object {
        val ZERO = Vec2(0f, 0f)
    }
}

class PongGame {
    var isAuthority: Boolean by mutableStateOf(false)
        private set
    var myPaddleX: Float by mutableStateOf(0f)
        private set
    var opponentPaddleX: Float by mutableStateOf(0f)
        private set
    var ballPos: Vec2 by mutableStateOf(Vec2.ZERO)
        private set
    var myScore: UShort by mutableStateOf(0u)
        private set
    var theirScore: UShort by mutableStateOf(0u)
        private set

    private var opponentPaddleVx: Float = 0f
    private var ballVel: Vec2 = Vec2.ZERO
    private var lastClockSec: Double? = null
    private var lastOpponentPaddleAtSec: Double? = null

    val opponentPaddlePredictedX: Float
        get() = clamp(opponentPaddleX + opponentPaddleVx * OPPONENT_LEAD_TIME, -1f, 1f)

    fun produceTickFrames(myPaddleXIn: Float): Pair<ByteArray, ByteArray?> {
        setMyPaddle(myPaddleXIn)
        tickFromClock()
        return Pair(WireFormat.encodePaddle(myPaddleX), ballFrame())
    }

    fun receivedOpponentPaddle(x: Float) {
        val clamped = clamp(x, -1f, 1f)
        val now = nowSec()
        val last = lastOpponentPaddleAtSec
        if (last != null && now > last) {
            val dt = min((now - last).toFloat(), 0.2f)
            if (dt > 0.005f) {
                val raw = (clamped - opponentPaddleX) / dt
                val capped = clamp(raw, -OPPONENT_VELOCITY_CAP, OPPONENT_VELOCITY_CAP)
                opponentPaddleVx = opponentPaddleVx * (1f - OPPONENT_VELOCITY_EMA) +
                    capped * OPPONENT_VELOCITY_EMA
            }
        } else {
            opponentPaddleVx = 0f
        }
        opponentPaddleX = clamped
        lastOpponentPaddleAtSec = now
    }

    fun receivedBall(payload: WireFormat.BallPayload) {
        ballPos = Vec2(payload.x, -payload.y)
        ballVel = Vec2(payload.vx, -payload.vy)
        theirScore = payload.myScore
        myScore = payload.theirScore
    }

    fun resetForNewSession(asAuthority: Boolean) {
        isAuthority = asAuthority
        myScore = 0u
        theirScore = 0u
        opponentPaddleX = 0f
        opponentPaddleVx = 0f
        lastClockSec = null
        lastOpponentPaddleAtSec = null
        if (asAuthority) {
            resetBall(towardMe = Random.nextBoolean())
        } else {
            ballPos = Vec2.ZERO
            ballVel = Vec2.ZERO
        }
    }

    fun sessionEnded() {
        opponentPaddleX = 0f
        opponentPaddleVx = 0f
        lastOpponentPaddleAtSec = null
        ballPos = Vec2.ZERO
        ballVel = Vec2.ZERO
    }

    private fun setMyPaddle(x: Float) {
        myPaddleX = clamp(x, -1f, 1f)
    }

    private fun tickFromClock() {
        val now = nowSec()
        val last = lastClockSec
        val dt = if (last != null && now > last) min((now - last).toFloat(), 0.05f) else 1f / 60f
        lastClockSec = now
        tick(dt)
    }

    private fun tick(dt: Float) {
        if (!isAuthority) {
            ballPos = ballPos + ballVel * dt
            return
        }
        var pos = ballPos + ballVel * dt

        if (pos.x < -1f + BALL_RADIUS) {
            pos = Vec2(-1f + BALL_RADIUS, pos.y)
            ballVel = Vec2(kotlin.math.abs(ballVel.x), ballVel.y)
        } else if (pos.x > 1f - BALL_RADIUS) {
            pos = Vec2(1f - BALL_RADIUS, pos.y)
            ballVel = Vec2(-kotlin.math.abs(ballVel.x), ballVel.y)
        }

        if (ballVel.y > 0f && pos.y > MY_PADDLE_Y - BALL_RADIUS) {
            if (kotlin.math.abs(pos.x - myPaddleX) < PADDLE_HALF_WIDTH + BALL_RADIUS) {
                pos = Vec2(pos.x, MY_PADDLE_Y - BALL_RADIUS)
                ballVel = Vec2(ballVel.x, -kotlin.math.abs(ballVel.y))
                applyPaddleSpin(ballX = pos.x, paddleX = myPaddleX)
                speedUp()
            }
        }
        if (ballVel.y < 0f && pos.y < OPPONENT_PADDLE_Y + BALL_RADIUS) {
            val leadX = opponentPaddlePredictedX
            if (kotlin.math.abs(pos.x - leadX) < PADDLE_HALF_WIDTH + BALL_RADIUS) {
                pos = Vec2(pos.x, OPPONENT_PADDLE_Y + BALL_RADIUS)
                ballVel = Vec2(ballVel.x, kotlin.math.abs(ballVel.y))
                applyPaddleSpin(ballX = pos.x, paddleX = leadX)
                speedUp()
            }
        }

        if (pos.y > 1f + BALL_RADIUS) {
            theirScore = (theirScore.toInt() + 1).toUShort()
            resetBall(towardMe = true)
            return
        }
        if (pos.y < -1f - BALL_RADIUS) {
            myScore = (myScore.toInt() + 1).toUShort()
            resetBall(towardMe = false)
            return
        }
        ballPos = pos
    }

    private fun ballFrame(): ByteArray? {
        if (!isAuthority) return null
        return WireFormat.encodeBall(
            x = ballPos.x,
            y = ballPos.y,
            vx = ballVel.x,
            vy = ballVel.y,
            myScore = myScore,
            theirScore = theirScore,
        )
    }

    private fun resetBall(towardMe: Boolean) {
        ballPos = Vec2.ZERO
        val angle = (Random.nextFloat() - 0.5f) * 0.7f
        val vx = sin(angle) * INITIAL_BALL_SPEED
        val vy = (if (towardMe) 1f else -1f) * cos(angle) * INITIAL_BALL_SPEED
        ballVel = Vec2(vx, vy)
    }

    private fun applyPaddleSpin(ballX: Float, paddleX: Float) {
        val offset = (ballX - paddleX) / (PADDLE_HALF_WIDTH + BALL_RADIUS)
        val clamped = clamp(offset, -1f, 1f)
        val speed = sqrt(ballVel.x * ballVel.x + ballVel.y * ballVel.y)
        val newAngle = clamped * 0.9f
        val signY = if (ballVel.y >= 0f) 1f else -1f
        ballVel = Vec2(sin(newAngle) * speed, signY * cos(newAngle) * speed)
    }

    private fun speedUp() {
        val speed = sqrt(ballVel.x * ballVel.x + ballVel.y * ballVel.y)
        if (speed <= 0.0001f) return
        val next = min(MAX_BALL_SPEED, speed * SPEEDUP_FACTOR)
        ballVel = ballVel * (next / speed)
    }

    private fun nowSec(): Double = System.nanoTime() / 1_000_000_000.0

    companion object {
        const val PADDLE_HALF_WIDTH = 0.15f
        const val MY_PADDLE_Y = 0.92f
        const val OPPONENT_PADDLE_Y = -0.92f
        const val BALL_RADIUS = 0.035f
        const val INITIAL_BALL_SPEED = 0.7f
        const val MAX_BALL_SPEED = 1.8f
        const val SPEEDUP_FACTOR = 1.05f
        const val OPPONENT_LEAD_TIME = 0.05f
        const val OPPONENT_VELOCITY_EMA = 0.3f
        const val OPPONENT_VELOCITY_CAP = 4.0f
    }
}

private fun clamp(v: Float, lo: Float, hi: Float): Float = max(lo, min(hi, v))
