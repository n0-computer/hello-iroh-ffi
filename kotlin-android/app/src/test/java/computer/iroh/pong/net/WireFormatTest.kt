package computer.iroh.pong.net

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WireFormatTest {

    @Test
    fun alpnMatchesSwift() {
        // The Swift app uses the same literal byte string. If anyone bumps
        // the protocol on either side, both must move together.
        assertArrayEquals(
            "iroh-helloiroh-pong/0".toByteArray(Charsets.UTF_8),
            WireFormat.ALPN,
        )
    }

    @Test
    fun paddleFrameSize() {
        assertEquals(5, WireFormat.encodePaddle(0f).size)
        assertEquals(5, WireFormat.encodePaddle(-1f).size)
        assertEquals(5, WireFormat.encodePaddle(1f).size)
    }

    @Test
    fun paddleFrameTagAndLittleEndian() {
        // Tag in byte 0, then x as little-endian f32 bits.
        val frame = WireFormat.encodePaddle(0.5f)
        assertEquals(WireFormat.TAG_PADDLE, frame[0])

        // 0.5f bit pattern = 0x3F000000 (big-endian); little-endian on the
        // wire means bytes 1..4 are 00, 00, 00, 3F.
        assertEquals(0x00.toByte(), frame[1])
        assertEquals(0x00.toByte(), frame[2])
        assertEquals(0x00.toByte(), frame[3])
        assertEquals(0x3F.toByte(), frame[4])
    }

    @Test
    fun paddleRoundTrip() {
        for (x in listOf(-1f, -0.37f, 0f, 0.42f, 1f)) {
            val body = WireFormat.encodePaddle(x).drop(1).toByteArray()
            assertEquals(x, WireFormat.decodePaddleBody(body))
        }
    }

    @Test
    fun decodePaddleRejectsWrongLength() {
        assertNull(WireFormat.decodePaddleBody(ByteArray(0)))
        assertNull(WireFormat.decodePaddleBody(ByteArray(3)))
        assertNull(WireFormat.decodePaddleBody(ByteArray(5)))
    }

    @Test
    fun ballFrameSize() {
        val frame = WireFormat.encodeBall(
            x = 0f,
            y = 0f,
            vx = 0f,
            vy = 0f,
            myScore = 0u,
            theirScore = 0u,
        )
        assertEquals(21, frame.size)
    }

    @Test
    fun ballRoundTrip() {
        val original = WireFormat.BallPayload(
            x = 0.123f,
            y = -0.456f,
            vx = 0.789f,
            vy = -1.234f,
            myScore = 3u,
            theirScore = 5u,
        )
        val frame = WireFormat.encodeBall(
            x = original.x,
            y = original.y,
            vx = original.vx,
            vy = original.vy,
            myScore = original.myScore,
            theirScore = original.theirScore,
        )
        assertEquals(WireFormat.TAG_BALL, frame[0])
        val decoded = WireFormat.decodeBallBody(frame.drop(1).toByteArray())
        assertEquals(original, decoded)
    }

    @Test
    fun decodeBallRejectsWrongLength() {
        assertNull(WireFormat.decodeBallBody(ByteArray(0)))
        assertNull(WireFormat.decodeBallBody(ByteArray(19)))
        assertNull(WireFormat.decodeBallBody(ByteArray(21)))
    }

    @Test
    fun ballScoresArePreservedAtMaxUShort() {
        // UShort max should round-trip without truncation.
        val frame = WireFormat.encodeBall(
            x = 0f, y = 0f, vx = 0f, vy = 0f,
            myScore = UShort.MAX_VALUE,
            theirScore = UShort.MAX_VALUE,
        )
        val decoded = WireFormat.decodeBallBody(frame.drop(1).toByteArray())!!
        assertEquals(UShort.MAX_VALUE, decoded.myScore)
        assertEquals(UShort.MAX_VALUE, decoded.theirScore)
    }
}
