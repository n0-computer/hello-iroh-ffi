import Foundation
import Observation

@MainActor
@Observable
final class DotGame {
    static let dotRadius: Float = 0.045

    var myPos: SIMD2<Float> = .zero
    var theirPos: SIMD2<Float> = .zero

    func setMyPos(x: Float, y: Float) {
        myPos = SIMD2(clamp(x), clamp(y))
    }

    func receivedTheirPos(x: Float, y: Float) {
        theirPos = SIMD2(clamp(x), clamp(y))
    }

    /// Recenter the peer's dot when a new session starts. The local dot keeps
    /// following motion, so it does not need resetting.
    func resetForNewSession() {
        theirPos = .zero
    }

    func sessionEnded() {
        theirPos = .zero
    }

    private func clamp(_ v: Float) -> Float {
        max(-1, min(1, v))
    }
}
