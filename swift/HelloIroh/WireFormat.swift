import Foundation

enum WireFormat {
    static let alpn: Data = Data("iroh-helloiroh-dot/0".utf8)

    static let positionFrameSize: UInt32 = 8   // x(4) + y(4)

    static func encodePosition(x: Float, y: Float) -> Data {
        var out = Data(count: 8)
        out.withUnsafeMutableBytes { raw in
            let base = raw.baseAddress!
            base.assumingMemoryBound(to: UInt32.self).pointee = x.bitPattern.littleEndian
            base.advanced(by: 4).assumingMemoryBound(to: UInt32.self).pointee = y.bitPattern.littleEndian
        }
        return out
    }

    struct Position {
        let x: Float
        let y: Float
    }

    static func decodePosition(_ data: Data) -> Position? {
        guard data.count == 8 else { return nil }
        return data.withUnsafeBytes { raw in
            let base = raw.baseAddress!
            let xBits = UInt32(littleEndian: base.assumingMemoryBound(to: UInt32.self).pointee)
            let yBits = UInt32(littleEndian: base.advanced(by: 4).assumingMemoryBound(to: UInt32.self).pointee)
            return Position(x: Float(bitPattern: xBits), y: Float(bitPattern: yBits))
        }
    }
}
