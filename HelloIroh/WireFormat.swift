import Foundation

enum WireFormat {
    static let alpn: Data = Data("iroh-helloiroh-pong/0".utf8)

    static let tagPaddle: UInt8 = 0
    static let tagBall: UInt8 = 1

    static let paddleFrameSize: UInt32 = 5   // tag(1) + x(4)
    static let ballFrameSize: UInt32 = 21    // tag(1) + x(4) + y(4) + vx(4) + vy(4) + myScore(2) + theirScore(2)

    static func encodePaddle(x: Float) -> Data {
        var out = Data(count: 5)
        out.withUnsafeMutableBytes { raw in
            raw[0] = tagPaddle
            let p = raw.baseAddress!.advanced(by: 1).assumingMemoryBound(to: UInt32.self)
            p.pointee = x.bitPattern.littleEndian
        }
        return out
    }

    static func decodePaddleBody(_ data: Data) -> Float? {
        guard data.count == 4 else { return nil }
        let bits: UInt32 = data.withUnsafeBytes { raw in
            UInt32(littleEndian: raw.load(as: UInt32.self))
        }
        return Float(bitPattern: bits)
    }

    static func encodeBall(x: Float, y: Float, vx: Float, vy: Float, myScore: UInt16, theirScore: UInt16) -> Data {
        var out = Data(count: 21)
        out.withUnsafeMutableBytes { raw in
            raw[0] = tagBall
            let base = raw.baseAddress!.advanced(by: 1)
            base.assumingMemoryBound(to: UInt32.self).pointee = x.bitPattern.littleEndian
            base.advanced(by: 4).assumingMemoryBound(to: UInt32.self).pointee = y.bitPattern.littleEndian
            base.advanced(by: 8).assumingMemoryBound(to: UInt32.self).pointee = vx.bitPattern.littleEndian
            base.advanced(by: 12).assumingMemoryBound(to: UInt32.self).pointee = vy.bitPattern.littleEndian
            base.advanced(by: 16).assumingMemoryBound(to: UInt16.self).pointee = myScore.littleEndian
            base.advanced(by: 18).assumingMemoryBound(to: UInt16.self).pointee = theirScore.littleEndian
        }
        return out
    }

    struct BallPayload {
        let x: Float
        let y: Float
        let vx: Float
        let vy: Float
        let myScore: UInt16
        let theirScore: UInt16
    }

    static func decodeBallBody(_ data: Data) -> BallPayload? {
        guard data.count == 20 else { return nil }
        return data.withUnsafeBytes { raw in
            let base = raw.baseAddress!
            let xBits = UInt32(littleEndian: base.assumingMemoryBound(to: UInt32.self).pointee)
            let yBits = UInt32(littleEndian: base.advanced(by: 4).assumingMemoryBound(to: UInt32.self).pointee)
            let vxBits = UInt32(littleEndian: base.advanced(by: 8).assumingMemoryBound(to: UInt32.self).pointee)
            let vyBits = UInt32(littleEndian: base.advanced(by: 12).assumingMemoryBound(to: UInt32.self).pointee)
            let my = UInt16(littleEndian: base.advanced(by: 16).assumingMemoryBound(to: UInt16.self).pointee)
            let their = UInt16(littleEndian: base.advanced(by: 18).assumingMemoryBound(to: UInt16.self).pointee)
            return BallPayload(
                x: Float(bitPattern: xBits),
                y: Float(bitPattern: yBits),
                vx: Float(bitPattern: vxBits),
                vy: Float(bitPattern: vyBits),
                myScore: my,
                theirScore: their
            )
        }
    }
}
