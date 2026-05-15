import Foundation

enum WireFormat {
    static let alpn: Data = Data("iroh-helloiroh-ball/0".utf8)
    static let frameSize: UInt32 = 8

    static func encode(x: Float, y: Float) -> Data {
        var out = Data(count: 8)
        out.withUnsafeMutableBytes { raw in
            let p = raw.bindMemory(to: UInt32.self)
            p[0] = x.bitPattern.littleEndian
            p[1] = y.bitPattern.littleEndian
        }
        return out
    }

    static func decode(_ data: Data) -> (x: Float, y: Float)? {
        guard data.count == 8 else { return nil }
        let (xBits, yBits): (UInt32, UInt32) = data.withUnsafeBytes { raw in
            let p = raw.bindMemory(to: UInt32.self)
            return (UInt32(littleEndian: p[0]), UInt32(littleEndian: p[1]))
        }
        return (Float(bitPattern: xBits), Float(bitPattern: yBits))
    }
}
