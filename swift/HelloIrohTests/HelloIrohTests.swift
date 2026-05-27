import Foundation
import Testing
@testable import HelloIroh

struct WireFormatTests {

    @Test func alpnMatchesKotlin() {
        // The Kotlin app encodes the same literal. Bump both together.
        #expect(WireFormat.alpn == Data("iroh-helloiroh-dot/0".utf8))
    }

    @Test func positionFrameIsEightBytes() {
        #expect(WireFormat.encodePosition(x: 0, y: 0).count == 8)
        #expect(WireFormat.positionFrameSize == 8)
    }

    @Test func positionFrameIsLittleEndian() {
        // 0.5f bit pattern is 0x3F000000; little-endian on the wire is
        // 00 00 00 3F for each f32.
        let frame = WireFormat.encodePosition(x: 0.5, y: 0.5)
        #expect(Array(frame) == [0x00, 0x00, 0x00, 0x3F, 0x00, 0x00, 0x00, 0x3F])
    }

    @Test func positionRoundTrips() {
        for (x, y) in [(-1 as Float, 1 as Float), (0, 0), (0.42, -0.37), (1, -1)] {
            let decoded = WireFormat.decodePosition(WireFormat.encodePosition(x: x, y: y))
            #expect(decoded?.x == x)
            #expect(decoded?.y == y)
        }
    }

    @Test func decodeRejectsWrongLength() {
        #expect(WireFormat.decodePosition(Data(count: 0)) == nil)
        #expect(WireFormat.decodePosition(Data(count: 7)) == nil)
        #expect(WireFormat.decodePosition(Data(count: 9)) == nil)
    }
}
