import SwiftUI

struct ContentView: View {
    @State private var motion = MotionSource()
    @State private var peer: IrohPeer?
    @State private var peerIdInput: String = ""
    @State private var apiSecretInput: String = ""

    var body: some View {
        VStack(spacing: 12) {
            if let peer {
                header(peer: peer)
                connectBar(peer: peer)
                telemetryBar(peer: peer)
                BallScene(
                    selfPosition: motion.position,
                    remotePosition: peer.remotePosition,
                    selfColor: BallColors.color(forEndpointId: peer.endpointId),
                    remoteColor: peer.remotePositionColor,
                    onDrag: dragHandler
                )
                .frame(minHeight: 320)
            } else {
                ProgressView("Starting iroh…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .padding()
        .task {
            if peer == nil {
                let p = IrohPeer(motion: motion)
                peer = p
                motion.start()
                await p.start()
            }
        }
    }

    @ViewBuilder
    private func header(peer: IrohPeer) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text("My id:")
                .font(.subheadline)
            Text(peer.endpointId.isEmpty ? "…" : peer.endpointId)
                .font(.system(.caption, design: .monospaced))
                .lineLimit(1)
                .truncationMode(.middle)
            Button("Copy") { peer.copyEndpointIdToClipboard() }
                .buttonStyle(.bordered)
                .disabled(peer.endpointId.isEmpty)
        }
    }

    @ViewBuilder
    private func connectBar(peer: IrohPeer) -> some View {
        HStack(spacing: 8) {
            TextField("Peer endpoint id", text: $peerIdInput)
                .textFieldStyle(.roundedBorder)
                .font(.system(.body, design: .monospaced))
                .autocorrectionDisabled(true)
                #if os(iOS)
                .textInputAutocapitalization(.never)
                #endif
            Button("Connect") {
                Task { await peer.connect(toEndpointIdHex: peerIdInput) }
            }
            .buttonStyle(.borderedProminent)
            .disabled(!isLikelyEndpointId(peerIdInput))
        }
        Text(statusLine(for: peer.state))
            .font(.caption)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func telemetryBar(peer: IrohPeer) -> some View {
        HStack(spacing: 8) {
            SecureField("iroh services API secret (services1…)", text: $apiSecretInput)
                .textFieldStyle(.roundedBorder)
                .font(.system(.caption, design: .monospaced))
                .autocorrectionDisabled(true)
                #if os(iOS)
                .textInputAutocapitalization(.never)
                #endif
            Button("Save") {
                Task { await peer.saveApiSecret(apiSecretInput) }
            }
            .buttonStyle(.bordered)
            .disabled(apiSecretInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
        Text(telemetryLine(for: peer.telemetry))
            .font(.caption2)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func telemetryLine(for state: IrohPeer.TelemetryState) -> String {
        switch state {
        case .off: return "telemetry off — paste an API secret from services.iroh.computer to enable"
        case .starting: return "telemetry: connecting…"
        case .active(let name): return "telemetry: pushing as \(name)"
        case .error(let msg): return "telemetry error: \(msg)"
        }
    }

    private var dragHandler: ((SIMD2<Float>) -> Void)? {
        #if os(macOS)
        return { motion.setFromDrag(normalized: $0) }
        #else
        return nil
        #endif
    }

    private func isLikelyEndpointId(_ s: String) -> Bool {
        let trimmed = s.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count == 64 else { return false }
        return trimmed.allSatisfy { $0.isHexDigit }
    }

    private func statusLine(for state: IrohPeer.ConnectionState) -> String {
        switch state {
        case .idle: return "idle"
        case .binding: return "binding…"
        case .ready: return "ready — paste a peer id and tap Connect, or wait for an incoming connection"
        case .connecting: return "connecting…"
        case .connected(let short): return "connected to \(short)"
        case .error(let msg): return "error: \(msg)"
        }
    }
}

private extension IrohPeer {
    var remotePositionColor: Color {
        if case .connected(let short) = state {
            return BallColors.color(forEndpointId: short)
        }
        return .gray
    }
}

#Preview {
    ContentView()
}
