import Foundation
import Observation

@MainActor
@Observable
final class PongGame {
    static let paddleHalfWidth: Float = 0.15
    static let myPaddleY: Float = 0.92
    static let opponentPaddleY: Float = -0.92
    static let ballRadius: Float = 0.035
    static let initialBallSpeed: Float = 0.7
    static let maxBallSpeed: Float = 1.8
    static let speedupFactor: Float = 1.05
    static let opponentLeadTime: Float = 0.05
    static let opponentVelocityEMA: Float = 0.3
    static let opponentVelocityCap: Float = 4.0

    var isAuthority: Bool = false
    var myPaddleX: Float = 0
    var opponentPaddleX: Float = 0
    var opponentPaddleVx: Float = 0
    var ballPos: SIMD2<Float> = .zero
    var myScore: UInt16 = 0
    var theirScore: UInt16 = 0

    var opponentPaddlePredictedX: Float {
        max(-1, min(1, opponentPaddleX + opponentPaddleVx * Self.opponentLeadTime))
    }

    private var ballVel: SIMD2<Float> = .zero
    private var lastClock: TimeInterval?
    private var lastOpponentPaddleAt: TimeInterval?

    func setMyPaddle(x: Float) {
        myPaddleX = max(-1, min(1, x))
    }

    func tickFromClock() {
        let now = Date().timeIntervalSinceReferenceDate
        let dt: Float
        if let last = lastClock, now > last {
            dt = Float(min(now - last, 0.05))
        } else {
            dt = 1.0 / 60.0
        }
        lastClock = now
        tick(dt: dt)
    }

    func produceTickFrames(myPaddleX x: Float) -> (paddle: Data, ball: Data?) {
        setMyPaddle(x: x)
        tickFromClock()
        return (
            WireFormat.encodePaddle(x: myPaddleX),
            ballFrame()
        )
    }

    func receivedOpponentPaddle(x: Float) {
        let clamped = max(-1, min(1, x))
        let now = Date().timeIntervalSinceReferenceDate
        if let last = lastOpponentPaddleAt, now > last {
            let dt = Float(min(now - last, 0.2))
            if dt > 0.005 {
                let raw = (clamped - opponentPaddleX) / dt
                let capped = max(-Self.opponentVelocityCap, min(Self.opponentVelocityCap, raw))
                opponentPaddleVx = opponentPaddleVx * (1 - Self.opponentVelocityEMA)
                    + capped * Self.opponentVelocityEMA
            }
        } else {
            opponentPaddleVx = 0
        }
        opponentPaddleX = clamped
        lastOpponentPaddleAt = now
    }

    func receivedBall(payload: WireFormat.BallPayload) {
        ballPos = SIMD2(payload.x, -payload.y)
        ballVel = SIMD2(payload.vx, -payload.vy)
        theirScore = payload.myScore
        myScore = payload.theirScore
    }

    func resetForNewSession(asAuthority: Bool) {
        isAuthority = asAuthority
        myScore = 0
        theirScore = 0
        opponentPaddleX = 0
        opponentPaddleVx = 0
        lastClock = nil
        lastOpponentPaddleAt = nil
        if asAuthority {
            resetBall(towardMe: Bool.random())
        } else {
            ballPos = .zero
            ballVel = .zero
        }
    }

    func sessionEnded() {
        opponentPaddleX = 0
        opponentPaddleVx = 0
        lastOpponentPaddleAt = nil
        ballPos = .zero
        ballVel = .zero
    }

    func tick(dt: Float) {
        guard isAuthority else {
            ballPos = ballPos + ballVel * dt
            return
        }
        var pos = ballPos + ballVel * dt

        if pos.x < -1 + Self.ballRadius {
            pos.x = -1 + Self.ballRadius
            ballVel.x = abs(ballVel.x)
        } else if pos.x > 1 - Self.ballRadius {
            pos.x = 1 - Self.ballRadius
            ballVel.x = -abs(ballVel.x)
        }

        if ballVel.y > 0 && pos.y > Self.myPaddleY - Self.ballRadius {
            if abs(pos.x - myPaddleX) < Self.paddleHalfWidth + Self.ballRadius {
                pos.y = Self.myPaddleY - Self.ballRadius
                ballVel.y = -abs(ballVel.y)
                applyPaddleSpin(ballX: pos.x, paddleX: myPaddleX)
                speedUp()
            }
        }
        if ballVel.y < 0 && pos.y < Self.opponentPaddleY + Self.ballRadius {
            let leadX = opponentPaddlePredictedX
            if abs(pos.x - leadX) < Self.paddleHalfWidth + Self.ballRadius {
                pos.y = Self.opponentPaddleY + Self.ballRadius
                ballVel.y = abs(ballVel.y)
                applyPaddleSpin(ballX: pos.x, paddleX: leadX)
                speedUp()
            }
        }

        if pos.y > 1 + Self.ballRadius {
            theirScore = theirScore &+ 1
            resetBall(towardMe: true)
            return
        }
        if pos.y < -1 - Self.ballRadius {
            myScore = myScore &+ 1
            resetBall(towardMe: false)
            return
        }
        ballPos = pos
    }

    func ballFrame() -> Data? {
        guard isAuthority else { return nil }
        return WireFormat.encodeBall(
            x: ballPos.x,
            y: ballPos.y,
            vx: ballVel.x,
            vy: ballVel.y,
            myScore: myScore,
            theirScore: theirScore
        )
    }

    private func resetBall(towardMe: Bool) {
        ballPos = .zero
        let angle = Float.random(in: -0.35...0.35)
        let vx = sin(angle) * Self.initialBallSpeed
        let vy = (towardMe ? 1 : -1) * cos(angle) * Self.initialBallSpeed
        ballVel = SIMD2(vx, vy)
    }

    private func applyPaddleSpin(ballX: Float, paddleX: Float) {
        let offset = (ballX - paddleX) / (Self.paddleHalfWidth + Self.ballRadius)
        let clamped = max(-1, min(1, offset))
        let speed = (ballVel.x * ballVel.x + ballVel.y * ballVel.y).squareRoot()
        let newAngle = clamped * 0.9
        let signY: Float = ballVel.y >= 0 ? 1 : -1
        ballVel = SIMD2(sin(newAngle) * speed, signY * cos(newAngle) * speed)
    }

    private func speedUp() {
        let speed = (ballVel.x * ballVel.x + ballVel.y * ballVel.y).squareRoot()
        guard speed > 0.0001 else { return }
        let next = min(Self.maxBallSpeed, speed * Self.speedupFactor)
        ballVel *= (next / speed)
    }
}
