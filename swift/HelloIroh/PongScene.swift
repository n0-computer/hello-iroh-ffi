import SwiftUI

struct PongScene: View {
    let game: PongGame
    let myColor: Color
    let opponentColor: Color
    var onDrag: ((Float) -> Void)?

    private let paddleHeight: CGFloat = 12

    var body: some View {
        GeometryReader { geo in
            let size = geo.size

            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(Color.secondary.opacity(0.08))

                centerLine(in: size)

                paddleView(
                    x: game.opponentPaddlePredictedX,
                    y: PongGame.opponentPaddleY,
                    color: opponentColor,
                    in: size
                )

                paddleView(
                    x: game.myPaddleX,
                    y: PongGame.myPaddleY,
                    color: myColor,
                    in: size
                )

                ballView(in: size)

                scoreOverlay(in: size)
            }
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .contentShape(RoundedRectangle(cornerRadius: 18))
            .gesture(dragGesture(size: size))
        }
    }

    @ViewBuilder
    private func centerLine(in size: CGSize) -> some View {
        Path { path in
            let mid = size.height / 2
            var x: CGFloat = 16
            while x < size.width - 16 {
                path.move(to: CGPoint(x: x, y: mid))
                path.addLine(to: CGPoint(x: min(x + 16, size.width - 16), y: mid))
                x += 28
            }
        }
        .stroke(Color.secondary.opacity(0.35), style: StrokeStyle(lineWidth: 2, lineCap: .round))
    }

    private func paddleView(x: Float, y: Float, color: Color, in size: CGSize) -> some View {
        let width = CGFloat(PongGame.paddleHalfWidth) * 2 * size.width
        return RoundedRectangle(cornerRadius: paddleHeight / 2)
            .fill(color)
            .frame(width: width, height: paddleHeight)
            .position(point(SIMD2(x, y), in: size))
    }

    private func ballView(in size: CGSize) -> some View {
        let diameter = CGFloat(PongGame.ballRadius) * 2 * min(size.width, size.height)
        return Circle()
            .fill(Color.primary)
            .frame(width: diameter, height: diameter)
            .position(point(game.ballPos, in: size))
    }

    @ViewBuilder
    private func scoreOverlay(in size: CGSize) -> some View {
        VStack {
            HStack {
                Text("\(game.theirScore)")
                    .font(.system(size: 28, weight: .bold, design: .monospaced))
                    .foregroundStyle(opponentColor)
                Spacer()
            }
            .padding(.horizontal, 18)
            .padding(.top, 10)
            Spacer()
            HStack {
                Spacer()
                Text("\(game.myScore)")
                    .font(.system(size: 28, weight: .bold, design: .monospaced))
                    .foregroundStyle(myColor)
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 10)
        }
        .allowsHitTesting(false)
    }

    private func point(_ p: SIMD2<Float>, in size: CGSize) -> CGPoint {
        let x = (CGFloat(p.x) + 1) * 0.5 * size.width
        let y = (CGFloat(p.y) + 1) * 0.5 * size.height
        return CGPoint(x: x, y: y)
    }

    private func dragGesture(size: CGSize) -> some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { value in
                guard let onDrag, size.width > 0 else { return }
                let nx = Float((value.location.x / size.width) * 2 - 1)
                onDrag(max(-1, min(1, nx)))
            }
    }
}

enum PongColors {
    static func color(forEndpointId hex: String) -> Color {
        let hash = hex.unicodeScalars.reduce(UInt32(0)) { acc, scalar in
            acc &* 31 &+ scalar.value
        }
        let hue = Double(hash % 360) / 360.0
        return Color(hue: hue, saturation: 0.7, brightness: 0.95)
    }
}
