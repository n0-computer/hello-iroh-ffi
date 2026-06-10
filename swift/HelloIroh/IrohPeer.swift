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

    enum TelemetryState: Equatable {
        case off
        case starting
        case active(name: String)
        case error(String)
    }

    let motion: MotionSource
    let game = DotGame()

    var endpointId: String = ""
    var state: ConnectionState = .idle
    /// One-line live view of the connection's open network paths
    /// (direct vs relay, address, RTT), rendered under the status line.
    var pathInfo: String = ""
    var telemetry: TelemetryState = .off
    var apiSecret: String = UserDefaults.standard.string(forKey: IrohPeer.apiSecretKey) ?? ""

    static let apiSecretKey = "iroh.helloiroh.apiSecret"
    static let defaultApiSecret = "servicesaaqg6nnf7kr3uiacviqgbxeqconvhuz4ldr5dem4gqhsp3cyat6qxexoctwjsi7m6dh2t2qvfu2yhdoaav6eibaj4aaavhonlixbohceu4aa"

    var isUsingDefaultApiSecret: Bool { apiSecret.isEmpty }

    private var endpoint: Endpoint?
    private var acceptTask: Task<Void, Never>?
    private var pathMonitorTask: Task<Void, Never>?
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

    func saveApiSecret(_ secret: String) async {
        let trimmed = secret.trimmingCharacters(in: .whitespacesAndNewlines)
        apiSecret = trimmed
        UserDefaults.standard.set(trimmed, forKey: Self.apiSecretKey)
        await startServicesClient()
    }

    private func startServicesClient() async {
        services = nil
        let secret = apiSecret.isEmpty ? Self.defaultApiSecret : apiSecret
        guard let ep = endpoint else { return }
        telemetry = .starting
        let name = deviceName()
        do {
            let client = try await ServicesClient.create(
                endpoint: ep,
                options: ServicesOptions(apiSecret: secret, name: name)
            )
            services = client
            telemetry = .active(name: name)
        } catch {
            telemetry = .error("\(error)")
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
            adoptSession(bi: bi, conn: conn, remoteIdHex: parsed.description)
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
                adoptSession(bi: bi, conn: conn, remoteIdHex: remoteId)
            } catch {
                continue
            }
        }
    }

    private func adoptSession(bi: BiStream, conn: Connection, remoteIdHex: String) {
        currentSession?.stop()
        game.resetForNewSession()
        startPathMonitor(conn)
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
        pathMonitorTask?.cancel()
        pathMonitorTask = nil
        pathInfo = ""
        game.sessionEnded()
        if case .connected = state { state = .ready }
    }

    /// Polls the connection's path snapshots once a second and renders them
    /// as one line, so direct-vs-relay and RTT are visible in the UI. The
    /// selected path (the one carrying application data) is marked with "*".
    private func startPathMonitor(_ conn: Connection) {
        pathMonitorTask?.cancel()
        pathMonitorTask = Task { [weak self] in
            while !Task.isCancelled {
                let line = conn.paths().map { p in
                    let kind = p.isRelay ? "relay" : (p.isIp ? "direct" : "?")
                    let mark = p.isSelected ? "* " : ""
                    return "\(mark)\(kind) \(p.remoteAddr) \(p.rttMs)ms"
                }.joined(separator: " | ")
                await MainActor.run { self?.pathInfo = line }
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }
    }
}
