import Foundation
import IrohLib

final class PeerSession: @unchecked Sendable {
    typealias FrameProducer = @Sendable () async -> Data

    private let bi: BiStream
    private let produceFrame: FrameProducer
    private let onPositionReceived: @Sendable (WireFormat.Position) -> Void
    private let onClosed: @Sendable () -> Void

    private var sendTask: Task<Void, Never>?
    private var recvTask: Task<Void, Never>?

    init(
        bi: BiStream,
        produceFrame: @escaping FrameProducer,
        onPositionReceived: @escaping @Sendable (WireFormat.Position) -> Void,
        onClosed: @escaping @Sendable () -> Void
    ) {
        self.bi = bi
        self.produceFrame = produceFrame
        self.onPositionReceived = onPositionReceived
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
            let frame = await produceFrame()
            do {
                try await send.writeAll(buf: frame)
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
                let body = try await recv.readExact(size: WireFormat.positionFrameSize)
                guard let pos = WireFormat.decodePosition(body) else { break }
                onPositionReceived(pos)
            } catch {
                break
            }
        }
        onClosed()
    }
}
