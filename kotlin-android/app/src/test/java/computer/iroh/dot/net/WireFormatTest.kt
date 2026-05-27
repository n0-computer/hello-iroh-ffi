package computer.iroh.dot.net

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
            "iroh-helloiroh-dot/0".toByteArray(Charsets.UTF_8),
            WireFormat.ALPN,
        )
    }

    @Test
    fun positionFrameSize() {
        assertEquals(8, WireFormat.encodePosition(0f, 0f).size)
        assertEquals(8, WireFormat.encodePosition(-1f, 1f).size)
    }

    @Test
    fun positionFrameLittleEndian() {
        // 0.5f bit pattern = 0x3F000000 (big-endian); little-endian on the
        // wire means each f32 is 00, 00, 00, 3F.
        val frame = WireFormat.encodePosition(0.5f, 0.5f)
        assertArrayEquals(
            byteArrayOf(0x00, 0x00, 0x00, 0x3F, 0x00, 0x00, 0x00, 0x3F),
            frame,
        )
    }

    @Test
    fun positionRoundTrip() {
        val cases = listOf(
            -1f to 1f,
            0f to 0f,
            0.42f to -0.37f,
            1f to -1f,
        )
        for ((x, y) in cases) {
            val decoded = WireFormat.decodePosition(WireFormat.encodePosition(x, y))
            assertEquals(WireFormat.Position(x, y), decoded)
        }
    }

    @Test
    fun decodeRejectsWrongLength() {
        assertNull(WireFormat.decodePosition(ByteArray(0)))
        assertNull(WireFormat.decodePosition(ByteArray(7)))
        assertNull(WireFormat.decodePosition(ByteArray(9)))
    }
}
