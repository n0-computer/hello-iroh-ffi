import SwiftUI

struct DotScene: View {
    let game: DotGame
    let myColor: Color
    let opponentColor: Color
    var onDrag: ((Float, Float) -> Void)?

    var body: some View {
        GeometryReader { geo in
            let size = geo.size

            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(Color.secondary.opacity(0.08))

                dotView(pos: game.theirPos, color: opponentColor, in: size)
                dotView(pos: game.myPos, color: myColor, in: size)
            }
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .contentShape(RoundedRectangle(cornerRadius: 18))
            .gesture(dragGesture(size: size))
        }
    }

    private func dotView(pos: SIMD2<Float>, color: Color, in size: CGSize) -> some View {
        let diameter = CGFloat(DotGame.dotRadius) * 2 * min(size.width, size.height)
        return Circle()
            .fill(color)
            .frame(width: diameter, height: diameter)
            .position(point(pos, in: size))
    }

    private func point(_ p: SIMD2<Float>, in size: CGSize) -> CGPoint {
        let x = (CGFloat(p.x) + 1) * 0.5 * size.width
        let y = (CGFloat(p.y) + 1) * 0.5 * size.height
        return CGPoint(x: x, y: y)
    }

    private func dragGesture(size: CGSize) -> some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { value in
                guard let onDrag, size.width > 0, size.height > 0 else { return }
                let nx = Float((value.location.x / size.width) * 2 - 1)
                let ny = Float((value.location.y / size.height) * 2 - 1)
                onDrag(max(-1, min(1, nx)), max(-1, min(1, ny)))
            }
    }
}

enum DotColors {
    static func color(forEndpointId hex: String) -> Color {
        let hash = hex.unicodeScalars.reduce(UInt32(0)) { acc, scalar in
            acc &* 31 &+ scalar.value
        }
        let hue = Double(hash % 360) / 360.0
        return Color(hue: hue, saturation: 0.7, brightness: 0.95)
    }
}
