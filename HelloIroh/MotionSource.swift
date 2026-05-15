import Foundation
import Observation
#if os(iOS)
import CoreMotion
#endif

@MainActor
@Observable
final class MotionSource {
    var position: SIMD2<Float> = .zero

    #if os(iOS)
    private let manager = CMMotionManager()
    #endif

    func start() {
        #if os(iOS)
        guard manager.isDeviceMotionAvailable else { return }
        manager.deviceMotionUpdateInterval = 1.0 / 60.0
        manager.startDeviceMotionUpdates(to: .main) { [weak self] motion, _ in
            guard let self, let g = motion?.gravity else { return }
            let x = Float(max(-1.0, min(1.0, g.x)))
            let y = Float(max(-1.0, min(1.0, -g.y)))
            self.position = SIMD2(x, y)
        }
        #endif
    }

    func stop() {
        #if os(iOS)
        manager.stopDeviceMotionUpdates()
        #endif
    }

    func setFromDrag(normalized: SIMD2<Float>) {
        position = SIMD2(
            max(-1, min(1, normalized.x)),
            max(-1, min(1, normalized.y))
        )
    }
}
