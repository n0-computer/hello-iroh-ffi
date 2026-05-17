import SwiftUI

struct SettingsView: View {
    let peer: IrohPeer
    @Environment(\.dismiss) private var dismiss
    @State private var apiSecretInput: String = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    SecureField("services1…", text: $apiSecretInput)
                        .font(.system(.body, design: .monospaced))
                        .autocorrectionDisabled(true)
                        #if os(iOS)
                        .textInputAutocapitalization(.never)
                        #endif
                    HStack {
                        Button("Save") {
                            Task { await peer.saveApiSecret(apiSecretInput) }
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(saveDisabled)
                        Button("Clear") {
                            apiSecretInput = ""
                            Task { await peer.saveApiSecret("") }
                        }
                        .buttonStyle(.bordered)
                        .disabled(peer.apiSecret.isEmpty && apiSecretInput.isEmpty)
                    }
                } header: {
                    Text("iroh services API key")
                } footer: {
                    if peer.isUsingDefaultApiSecret {
                        Text("Using bundled default key. Paste a secret from services.iroh.computer to override; stored locally on this device only.")
                    } else {
                        Text("Using a custom key. Tap Clear to revert to the bundled default.")
                    }
                }

                Section("Telemetry status") {
                    Text(telemetryLine(for: peer.telemetry))
                        .font(.callout)
                        .foregroundStyle(.secondary)
                }
            }
            .formStyle(.grouped)
            .navigationTitle("Settings")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .onAppear { apiSecretInput = peer.apiSecret }
        #if os(macOS)
        .frame(minWidth: 420, minHeight: 320)
        #endif
    }

    private var saveDisabled: Bool {
        let trimmed = apiSecretInput.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty || trimmed == peer.apiSecret
    }

    private func telemetryLine(for state: IrohPeer.TelemetryState) -> String {
        switch state {
        case .off: return "off — paste an API secret to enable"
        case .starting: return "connecting…"
        case .active(let name): return "active — pushing as \(name)"
        case .error(let msg): return "error: \(msg)"
        }
    }
}
