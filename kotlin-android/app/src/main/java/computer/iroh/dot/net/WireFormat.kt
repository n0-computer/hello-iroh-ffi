package computer.iroh.dot.net

import java.nio.ByteBuffer
import java.nio.ByteOrder

object WireFormat {
    val ALPN: ByteArray = "iroh-helloiroh-dot/0".toByteArray(Charsets.UTF_8)

    const val POSITION_FRAME_SIZE: UInt = 8u // x(4) + y(4)

    data class Position(val x: Float, val y: Float)

    fun encodePosition(x: Float, y: Float): ByteArray {
        val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        buf.putFloat(x)
        buf.putFloat(y)
        return buf.array()
    }

    fun decodePosition(body: ByteArray): Position? {
        if (body.size != 8) return null
        val buf = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN)
        return Position(x = buf.float, y = buf.float)
    }
}
