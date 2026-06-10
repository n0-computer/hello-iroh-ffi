import SwiftUI

struct ContentView: View {
    @State private var motion = MotionSource()
    @State private var peer: IrohPeer?
    @State private var peerIdInput: String = ""
    @State private var showingSettings = false

    var body: some View {
        let _ = NSLog("[HelloIroh] checkpoint C: ContentView.body, peer=%@", peer == nil ? "nil" : "set")
        VStack(spacing: 12) {
            if let peer {
                header(peer: peer)
                connectBar(peer: peer)
                DotScene(
                    game: peer.game,
                    myColor: DotColors.color(forEndpointId: peer.endpointId),
                    opponentColor: peer.opponentColor,
                    onDrag: dragHandler
                )
                .frame(minHeight: 360)
            } else {
                ProgressView("Starting iroh…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .padding()
        .task {
            NSLog("[HelloIroh] checkpoint D: .task fired")
            if peer == nil {
                NSLog("[HelloIroh] checkpoint E: creating IrohPeer")
                let p = IrohPeer(motion: motion)
                peer = p
                motion.start()
                NSLog("[HelloIroh] checkpoint F: about to call peer.start()")
                await p.start()
                NSLog("[HelloIroh] checkpoint G: peer.start() returned")
            }
        }
        .sheet(isPresented: $showingSettings) {
            if let peer {
                SettingsView(peer: peer)
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
            Spacer(minLength: 0)
            Button {
                showingSettings = true
            } label: {
                Image(systemName: "gearshape")
            }
            .buttonStyle(.bordered)
            .accessibilityLabel("Settings")
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

    private var dragHandler: ((Float, Float) -> Void)? {
        #if os(macOS)
        return { motion.setFromDrag(x: $0, y: $1) }
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
    var opponentColor: Color {
        if case .connected(let short) = state {
            return DotColors.color(forEndpointId: short)
        }
        return .gray
    }
}

#Preview {
    ContentView()
}
