import Foundation
import IrohLib

final class PeerSession: @unchecked Sendable {
    typealias FrameProducer = @Sendable () async -> (paddle: Data, ball: Data?)

    private let bi: BiStream
    private let produceFrames: FrameProducer
    private let onPaddleReceived: @Sendable (Float) -> Void
    private let onBallReceived: @Sendable (WireFormat.BallPayload) -> Void
    private let onClosed: @Sendable () -> Void

    private var sendTask: Task<Void, Never>?
    private var recvTask: Task<Void, Never>?

    init(
        bi: BiStream,
        produceFrames: @escaping FrameProducer,
        onPaddleReceived: @escaping @Sendable (Float) -> Void,
        onBallReceived: @escaping @Sendable (WireFormat.BallPayload) -> Void,
        onClosed: @escaping @Sendable () -> Void
    ) {
        self.bi = bi
        self.produceFrames = produceFrames
        self.onPaddleReceived = onPaddleReceived
        self.onBallReceived = onBallReceived
        self.onClosed = onClosed
    }

    func start() {
        sendTask = Task.detached { [weak self] in
            await self?.runSendLoop()
        }
        recvTask = Task.detached { [weak self] in
            await self?.runRecvLoop()
        }
    }

    func stop() {
        sendTask?.cancel()
        recvTask?.cancel()
    }

    private func runSendLoop() async {
        let send = bi.send()
        let tickNs: UInt64 = 16_666_000
        while !Task.isCancelled {
            let (paddle, ball) = await produceFrames()
            do {
                try await send.writeAll(buf: paddle)
                if let ball {
                    try await send.writeAll(buf: ball)
                }
            } catch {
                break
            }
            try? await Task.sleep(nanoseconds: tickNs)
        }
        onClosed()
    }

    private func runRecvLoop() async {
        let recv = bi.recv()
        while !Task.isCancelled {
            do {
                let tagData = try await recv.readExact(size: 1)
                guard let tag = tagData.first else { break }
                switch tag {
                case WireFormat.tagPaddle:
                    let body = try await recv.readExact(size: WireFormat.paddleFrameSize - 1)
                    if let x = WireFormat.decodePaddleBody(body) {
                        onPaddleReceived(x)
                    }
                case WireFormat.tagBall:
                    let body = try await recv.readExact(size: WireFormat.ballFrameSize - 1)
                    if let payload = WireFormat.decodeBallBody(body) {
                        onBallReceived(payload)
                    }
                default:
                    return
                }
            } catch {
                break
            }
        }
        onClosed()
    }
}
