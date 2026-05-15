import Foundation
import IrohLib

final class PeerSession: @unchecked Sendable {
    private let bi: BiStream
    private let getLocalPosition: @Sendable () async -> SIMD2<Float>
    private let onRemotePosition: @Sendable (SIMD2<Float>) -> Void
    private let onClosed: @Sendable () -> Void

    private var sendTask: Task<Void, Never>?
    private var recvTask: Task<Void, Never>?

    init(
        bi: BiStream,
        getLocalPosition: @escaping @Sendable () async -> SIMD2<Float>,
        onRemotePosition: @escaping @Sendable (SIMD2<Float>) -> Void,
        onClosed: @escaping @Sendable () -> Void
    ) {
        self.bi = bi
        self.getLocalPosition = getLocalPosition
        self.onRemotePosition = onRemotePosition
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
        let tickNs: UInt64 = 33_000_000
        while !Task.isCancelled {
            let pos = await getLocalPosition()
            let frame = WireFormat.encode(x: pos.x, y: pos.y)
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
                let frame = try await recv.readExact(size: WireFormat.frameSize)
                if let (x, y) = WireFormat.decode(frame) {
                    onRemotePosition(SIMD2(x, y))
                }
            } catch {
                break
            }
        }
        onClosed()
    }
}
