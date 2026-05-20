package computer.iroh.pong.net

import java.nio.ByteBuffer
import java.nio.ByteOrder

object WireFormat {
    val ALPN: ByteArray = "iroh-helloiroh-pong/0".toByteArray(Charsets.UTF_8)

    const val TAG_PADDLE: Byte = 0
    const val TAG_BALL: Byte = 1

    const val PADDLE_FRAME_SIZE: UInt = 5u
    const val BALL_FRAME_SIZE: UInt = 21u

    data class BallPayload(
        val x: Float,
        val y: Float,
        val vx: Float,
        val vy: Float,
        val myScore: UShort,
        val theirScore: UShort,
    )

    fun encodePaddle(x: Float): ByteArray {
        val buf = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(TAG_PADDLE)
        buf.putFloat(x)
        return buf.array()
    }

    fun decodePaddleBody(body: ByteArray): Float? {
        if (body.size != 4) return null
        return ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN).float
    }

    fun encodeBall(
        x: Float,
        y: Float,
        vx: Float,
        vy: Float,
        myScore: UShort,
        theirScore: UShort,
    ): ByteArray {
        val buf = ByteBuffer.allocate(21).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(TAG_BALL)
        buf.putFloat(x)
        buf.putFloat(y)
        buf.putFloat(vx)
        buf.putFloat(vy)
        buf.putShort(myScore.toShort())
        buf.putShort(theirScore.toShort())
        return buf.array()
    }

    fun decodeBallBody(body: ByteArray): BallPayload? {
        if (body.size != 20) return null
        val buf = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN)
        return BallPayload(
            x = buf.float,
            y = buf.float,
            vx = buf.float,
            vy = buf.float,
            myScore = buf.short.toUShort(),
            theirScore = buf.short.toUShort(),
        )
    }
}
