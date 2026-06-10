import Foundation
import Observation
#if os(iOS)
import CoreMotion
#endif

@MainActor
@Observable
final class MotionSource {
    var x: Float = 0
    var y: Float = 0

    #if os(iOS)
    private let manager = CMMotionManager()
    #endif

    func start() {
        #if os(iOS)
        guard manager.isDeviceMotionAvailable else { return }
        manager.deviceMotionUpdateInterval = 1.0 / 60.0
        manager.startDeviceMotionUpdates(to: .main) { [weak self] motion, _ in
            guard let self, let g = motion?.gravity else { return }
            // Roll the dot toward the lowered edge, like a ball on a tray.
            self.x = Float(max(-1.0, min(1.0, g.x)))
            self.y = Float(max(-1.0, min(1.0, -g.y)))
        }
        #endif
    }

    func stop() {
        #if os(iOS)
        manager.stopDeviceMotionUpdates()
        #endif
    }

    func setFromDrag(x: Float, y: Float) {
        self.x = max(-1, min(1, x))
        self.y = max(-1, min(1, y))
    }
}
