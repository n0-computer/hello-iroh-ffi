import Foundation
import Observation
#if os(iOS)
import CoreMotion
#endif

@MainActor
@Observable
final class MotionSource {
    var paddleX: Float = 0

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
            self.paddleX = x
        }
        #endif
    }

    func stop() {
        #if os(iOS)
        manager.stopDeviceMotionUpdates()
        #endif
    }

    func setFromDrag(x: Float) {
        paddleX = max(-1, min(1, x))
    }
}
