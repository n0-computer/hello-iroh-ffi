import SwiftUI

struct BallScene: View {
    let selfPosition: SIMD2<Float>
    let remotePosition: SIMD2<Float>?
    let selfColor: Color
    let remoteColor: Color
    var onDrag: ((SIMD2<Float>) -> Void)?

    private let radius: CGFloat = 22

    var body: some View {
        GeometryReader { geo in
            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(Color.secondary.opacity(0.08))

                if let remote = remotePosition {
                    Circle()
                        .strokeBorder(remoteColor.opacity(0.9), lineWidth: 3)
                        .background(Circle().fill(remoteColor.opacity(0.25)))
                        .frame(width: radius * 2, height: radius * 2)
                        .position(point(remote, in: geo.size))
                }

                ballView(in: geo.size)
            }
            .clipShape(RoundedRectangle(cornerRadius: 18))
        }
    }

    @ViewBuilder
    private func ballView(in size: CGSize) -> some View {
        let ball = Circle()
            .fill(selfColor)
            .frame(width: radius * 2, height: radius * 2)
            .position(point(selfPosition, in: size))
        if onDrag != nil {
            ball.gesture(dragGesture(size: size))
        } else {
            ball
        }
    }

    private func point(_ p: SIMD2<Float>, in size: CGSize) -> CGPoint {
        let inset = radius
        let w = max(0, size.width - inset * 2)
        let h = max(0, size.height - inset * 2)
        let x = inset + (CGFloat(p.x) + 1) * 0.5 * w
        let y = inset + (CGFloat(p.y) + 1) * 0.5 * h
        return CGPoint(x: x, y: y)
    }

    private func dragGesture(size: CGSize) -> some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { value in
                guard let onDrag else { return }
                let inset = radius
                let w = max(1, size.width - inset * 2)
                let h = max(1, size.height - inset * 2)
                let nx = Float((value.location.x - inset) / w * 2 - 1)
                let ny = Float((value.location.y - inset) / h * 2 - 1)
                onDrag(SIMD2(nx, ny))
            }
    }
}

enum BallColors {
    static func color(forEndpointId hex: String) -> Color {
        let hash = hex.unicodeScalars.reduce(UInt32(0)) { acc, scalar in
            acc &* 31 &+ scalar.value
        }
        let hue = Double(hash % 360) / 360.0
        return Color(hue: hue, saturation: 0.7, brightness: 0.95)
    }
}
