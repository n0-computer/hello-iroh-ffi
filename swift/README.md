# Hello Iroh FFI — Swift (iOS + macOS)

A small iOS + macOS presence demo built on [iroh](https://github.com/n0-computer/iroh) via the [iroh-ffi](https://github.com/n0-computer/iroh-ffi) Swift bindings.

Two peers connect over an iroh bi-directional stream, and each controls one dot in a shared coordinate space. Your dot moves with device tilt on iOS (CoreMotion gravity) or a mouse drag on macOS, and the other peer sees it move in real time. Discovery is manual: copy your endpoint id, paste it on the other peer, tap Connect.

The endpoint id is persisted across launches, so the copy/paste happens once and the demo keeps working session over session.

## Prerequisites

- macOS with [Xcode](https://developer.apple.com/xcode/) 16 or newer
- A free Apple Developer account if you want to run on a physical iPhone

That's it. The Xcode project consumes iroh-ffi as a remote Swift Package pinned to `1.0.0`; SPM downloads a prebuilt xcframework, so no Rust toolchain or iroh-ffi checkout is needed.

## Build and run

Open `HelloIroh.xcodeproj` in Xcode and let it finish resolving the Swift Package on first launch. The project has a single multiplatform SwiftUI target.

- **macOS**: select **My Mac** and Run.
- **iOS Simulator**: pick any iPhone simulator destination and Run.
- **iOS device**: select your device. Under **Signing & Capabilities**, switch the team to your own. The first time, trust the developer certificate under **Settings → General → VPN & Device Management** on the phone.

The app launches into a small UI: your full endpoint id at the top with a Copy button, a text field for the peer's endpoint id with a Connect button, and the dot field underneath.

## Play the demo

1. Build and run on two devices (e.g. your Mac and an iPhone, or two iPhones).
2. On device A, tap **Copy** and send the id to device B (Messages, AirDrop, whatever).
3. On device B, paste into the **Peer endpoint id** field and tap **Connect**.
4. Tilt your iPhone to move your dot: it rolls toward the lowered edge, like a ball on a tray. On the Mac, drag anywhere in the field and the dot follows your cursor.
5. Both dots appear in the same coordinate space, tinted by endpoint id, so you can watch the peer's dot track yours as either of you moves.
6. The line under the status shows the connection's live paths — direct vs relay, address, RTT, with `*` on the path carrying data. Watch it flip from relay to direct as iroh hole-punches.

### Wire format

Each peer streams its dot position over a single bi-directional stream at ~60 Hz. There is one frame type, so no tag byte is needed:

| Frame | Size | Layout |
|---|---|---|
| Position | 8 B | `f32 x, f32 y` |

Both coordinates are in `[-1, 1]`, little-endian, and shared directly: the peer renders your dot at the position you send. The receive loop reads a fixed 8 bytes per frame.

ALPN: `iroh-helloiroh-dot/0`.

### Telemetry (optional)

The app starts an iroh services client at boot so its metrics can show up in a [services.iroh.computer](https://services.iroh.computer) dashboard. The API key in `IrohPeer.swift` is a placeholder; paste your own to enable it. With the placeholder left in place the client fails to start and the demo works normally without telemetry.

## Project layout

```
swift/
├── HelloIroh/
│   ├── HelloIrohApp.swift     entry point
│   ├── ContentView.swift      assembles the UI
│   ├── IdentityStore.swift    persists the secret key in UserDefaults
│   ├── IrohPeer.swift         binds the Endpoint, runs the accept loop,
│   │                          owns DotGame, drives the session
│   ├── PeerSession.swift      position send/recv on one bi-stream
│   ├── DotGame.swift          my dot + peer's dot positions
│   ├── DotScene.swift         SwiftUI rendering of the two dots
│   ├── MotionSource.swift     iOS gravity / macOS drag → 2D position
│   ├── WireFormat.swift       ALPN + position frame encode/decode
│   └── HelloIroh.entitlements network sandbox entitlements (macOS)
└── HelloIroh.xcodeproj
```

## Configuration quirks worth knowing

These are wired up in the Xcode project already, but worth knowing if you're starting a similar project from scratch:

- **`-framework Network` linker flag on iOS.** iroh's Rust core uses Network.framework for interface enumeration on iOS. Without this, the iOS build fails with `Undefined symbols: _nw_interface_get_index`.
- **`com.apple.security.network.client` + `network.server` entitlements on macOS.** The App Sandbox blocks both inbound and outbound networking by default.
- **`NSMotionUsageDescription` on iOS.** CoreMotion's device motion API requires a usage description on iOS 17+.
- **`ENABLE_PREVIEWS = NO`.** Xcode 16's preview pipeline can't link `SwiftUICore.framework` when a Swift Package is in the graph (*"product being built is not an allowed client of it"*). Previews are off until Apple fixes this upstream.
- **No `NSLocalNetworkUsageDescription`.** Deliberately omitted: device testing showed iroh's QUIC unicast traffic does not engage the iOS local-network permission — direct LAN connections work without it, even with the app's Local Network toggle off. It only becomes necessary with mDNS-based discovery, which also needs the multicast entitlement.

## Further reading

- [docs.iroh.computer/languages/swift](https://docs.iroh.computer/languages/swift) — the from-scratch Swift project setup guide (this repo follows it)
- [docs.iroh.computer/concepts/endpoints](https://docs.iroh.computer/concepts/endpoints) — what endpoints, ids, and addresses actually are
- [iroh-ffi](https://github.com/n0-computer/iroh-ffi) — the Swift, Kotlin, Python, and Node bindings
