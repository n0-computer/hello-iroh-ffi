import Foundation
import Observation
import IrohLib
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

@MainActor
@Observable
final class IrohPeer {
    enum ConnectionState: Equatable {
        case idle
        case binding
        case ready
        case connecting
        case connected(peerShortId: String)
        case error(String)
    }

    let motion: MotionSource
    let game = DotGame()

    var endpointId: String = ""
    var state: ConnectionState = .idle

    /// Paste an API key from https://services.iroh.computer to see this
    /// device's metrics in your dashboard. With the placeholder left in
    /// place the services client fails to start; the demo works either way.
    static let apiSecret = "<your-iroh-services-api-key>"

    private var endpoint: Endpoint?
    private var acceptTask: Task<Void, Never>?
    private var currentSession: PeerSession?
    private var identity: IdentityStore?
    private var services: ServicesClient?

    init(motion: MotionSource) {
        self.motion = motion
    }

    func start() async {
        guard endpoint == nil else { return }
        state = .binding
        let identity = IdentityStore.loadOrCreate()
        self.identity = identity
        endpointId = identity.endpointId
        do {
            let ep = try await Endpoint.bind(options: EndpointOptions(
                preset: presetN0(),
                secretKey: identity.secretKey.toBytes(),
                alpns: [WireFormat.alpn]
            ))
            endpoint = ep
            state = .ready
            acceptTask = Task { [weak self] in
                await self?.runAcceptLoop(ep)
            }
            await startServicesClient()
        } catch {
            state = .error("bind failed: \(error)")
        }
    }

    private func startServicesClient() async {
        guard let ep = endpoint else { return }
        do {
            services = try await ServicesClient.create(
                endpoint: ep,
                options: ServicesOptions(apiSecret: Self.apiSecret, name: deviceName())
            )
        } catch {
            print("iroh services client not started: \(error)")
        }
    }

    private func deviceName() -> String {
        let short = String(endpointId.prefix(8))
        #if os(iOS)
        return "ios-\(short)"
        #elseif os(macOS)
        return "macos-\(short)"
        #else
        return "dot-\(short)"
        #endif
    }

    func connect(toEndpointIdHex hex: String) async {
        guard let ep = endpoint else { return }
        let trimmed = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let parsed = try? EndpointId.fromString(s: trimmed) else {
            state = .error("invalid endpoint id")
            return
        }
        state = .connecting
        let addr = EndpointAddr(id: parsed, relayUrl: nil, addresses: [])
        do {
            let conn = try await ep.connect(addr: addr, alpn: WireFormat.alpn)
            let bi = try await conn.openBi()
            adoptSession(bi: bi, remoteIdHex: parsed.description)
        } catch {
            state = .error("connect failed: \(error)")
        }
    }

    func copyEndpointIdToClipboard() {
        #if canImport(UIKit)
        UIPasteboard.general.string = endpointId
        #elseif canImport(AppKit)
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(endpointId, forType: .string)
        #endif
    }

    private func runAcceptLoop(_ ep: Endpoint) async {
        while !Task.isCancelled {
            guard let incoming = await ep.acceptNext() else { return }
            do {
                let accepting = try await incoming.accept()
                let alpn = try await accepting.alpn()
                guard alpn == WireFormat.alpn else { continue }
                let conn = try await accepting.connect()
                let bi = try await conn.acceptBi()
                let remoteId = conn.remoteId().description
                adoptSession(bi: bi, remoteIdHex: remoteId)
            } catch {
                continue
            }
        }
    }

    private func adoptSession(bi: BiStream, remoteIdHex: String) {
        currentSession?.stop()
        game.resetForNewSession()
        let game = self.game
        let motion = self.motion
        let session = PeerSession(
            bi: bi,
            produceFrame: {
                await MainActor.run {
                    game.setMyPos(x: motion.x, y: motion.y)
                    return WireFormat.encodePosition(x: game.myPos.x, y: game.myPos.y)
                }
            },
            onPositionReceived: { pos in
                Task { @MainActor in game.receivedTheirPos(x: pos.x, y: pos.y) }
            },
            onClosed: { [weak self] in
                Task { @MainActor in self?.handleSessionClosed() }
            }
        )
        currentSession = session
        session.start()
        let short = String(remoteIdHex.prefix(10))
        state = .connected(peerShortId: short)
    }

    private func handleSessionClosed() {
        guard currentSession != nil else { return }
        currentSession = nil
        game.sessionEnded()
        if case .connected = state { state = .ready }
    }
}
