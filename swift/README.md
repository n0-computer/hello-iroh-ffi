# iroh-pong — Swift (iOS + macOS)

A small iOS + macOS Pong demo built on [iroh](https://github.com/n0-computer/iroh) via the [iroh-ffi](https://github.com/n0-computer/iroh-ffi) Swift bindings.

Two peers connect over an iroh bi-directional stream and play a round of Pong. Paddles move with device tilt on iOS (CoreMotion gravity) or a mouse drag on macOS. Discovery is manual: copy your endpoint id, paste it on the other peer, tap Connect.

The endpoint id is persisted across launches, so the copy/paste happens once and the demo keeps working session over session.

## Prerequisites

- macOS with [Xcode](https://developer.apple.com/xcode/) 16 or newer
- [Rust](https://www.rust-lang.org/tools/install) with [`cargo-make`](https://crates.io/crates/cargo-make): `cargo install cargo-make`
- A free Apple Developer account if you want to run on a physical iPhone

The Xcode project consumes iroh-ffi as a **local** Swift Package at `../../iroh-ffi`. Until iroh-ffi tags a `1.0.0-rc` release whose pre-built xcframework matches the `feat-1-0` source, the project needs a sibling checkout so SPM can use the locally-built xcframework instead of falling through to the stale `v0.20.0` release zip.

## Setup

Clone iroh-ffi as a sibling of `iroh-pong` on the `feat-1-0` branch, install the Apple Rust targets, and build the xcframework:

```
parent-dir/
├── iroh-pong/
│   └── swift/             ← this directory
└── iroh-ffi/              ← on branch feat-1-0
```

```bash
git clone --branch feat-1-0 https://github.com/n0-computer/iroh-ffi
cd iroh-ffi
rustup target add \
  aarch64-apple-ios \
  aarch64-apple-ios-sim \
  x86_64-apple-ios \
  aarch64-apple-darwin
cargo make swift-xcframework
```

First build takes 5–15 minutes; subsequent builds reuse cargo's cache. The output goes into `iroh-ffi/IrohLib/artifacts/Iroh.xcframework`, which the Xcode project consumes via the local Swift Package.

## Build and run

Open `HelloIroh.xcodeproj` in Xcode and let it finish resolving the Swift Package on first launch. The project has a single multiplatform SwiftUI target.

- **macOS**: select **My Mac** and Run.
- **iOS Simulator**: pick any iPhone simulator destination and Run.
- **iOS device**: select your device, signed with your team. The first time, trust the developer certificate under **Settings → General → VPN & Device Management**.

The app launches into a small UI: your full endpoint id at the top with a Copy button and a gear (Settings), a text field for the peer's endpoint id with a Connect button, and the Pong field underneath.

## Play the demo

1. Build and run on two devices (e.g. your Mac and an iPhone, or two iPhones).
2. On device A, tap **Copy** and send the id to device B (Messages, AirDrop, whatever).
3. On device B, paste into the **Peer endpoint id** field and tap **Connect**.
4. The peer that tapped Connect is the **ball authority** — it simulates the ball physics and streams ball state. The other peer is paddle-only.
5. Tilt your iPhone left/right to move your paddle (at the bottom of your screen). On the Mac, drag anywhere in the field — paddle follows your cursor's x. Your opponent appears at the top of your screen.
6. First to 7 wins. The score resets when a new session starts.

### Wire format

Each peer streams over a single bi-directional stream at ~60 Hz with two tagged frame types:

| Tag | Frame | Size | Sender |
|---|---|---|---|
| `0` | Paddle: `f32 x` | 5 B | both peers |
| `1` | Ball: `f32 x, f32 y, f32 vx, f32 vy, u16 myScore, u16 theirScore` | 21 B | authority only |

All coordinates are in `[-1, 1]`. Each peer is rendered on the bottom of its own screen, so the y axis is flipped on receive (the x axis is shared). Velocity is included in the ball frame so the non-authority can extrapolate between snapshots; the authority lead-compensates the opponent paddle position (via a smoothed velocity estimate) when checking collisions, to offset network latency.

ALPN: `iroh-helloiroh-pong/0`.

### Settings

The gear button in the header opens a modal Settings sheet with an entry for an iroh services API key (stored in UserDefaults). A default key is bundled in source, so telemetry comes up automatically on a fresh install; paste your own secret to override it, or tap Clear to revert.

## Project layout

```
swift/
├── HelloIroh/
│   ├── HelloIrohApp.swift     entry point
│   ├── ContentView.swift      assembles the UI, owns the Settings sheet
│   ├── SettingsView.swift     API key entry + telemetry status
│   ├── IdentityStore.swift    persists the secret key in UserDefaults
│   ├── IrohPeer.swift         binds the Endpoint, runs the accept loop,
│   │                          owns PongGame and decides ball authority
│   ├── PeerSession.swift      tagged frame send/recv on one bi-stream
│   ├── PongGame.swift         paddles, ball, scores, physics, prediction
│   ├── PongScene.swift        SwiftUI rendering of field + paddles + ball
│   ├── MotionSource.swift     iOS gravity / macOS drag → 1D paddle x
│   ├── WireFormat.swift       ALPN + tagged frame encode/decode
│   └── HelloIroh.entitlements network sandbox entitlements (macOS)
└── HelloIroh.xcodeproj
```

## Configuration quirks worth knowing

These are wired up in the Xcode project already, but worth knowing if you're starting a similar project from scratch:

- **`-framework Network` linker flag on iOS.** iroh's Rust core uses Network.framework for interface enumeration on iOS. Without this, the iOS build fails with `Undefined symbols: _nw_interface_get_index`.
- **`com.apple.security.network.client` + `network.server` entitlements on macOS.** The App Sandbox blocks both inbound and outbound networking by default.
- **`NSMotionUsageDescription` on iOS.** CoreMotion's device motion API requires a usage description on iOS 17+.
- **`ENABLE_PREVIEWS = NO`.** Xcode 16's preview pipeline can't link `SwiftUICore.framework` when a Swift Package is in the graph (*"product being built is not an allowed client of it"*). Previews are off until Apple fixes this upstream.

## Further reading

- [docs.iroh.computer/languages/swift](https://docs.iroh.computer/languages/swift) — the from-scratch Swift project setup guide (this repo follows it)
- [docs.iroh.computer/concepts/endpoints](https://docs.iroh.computer/concepts/endpoints) — what endpoints, ids, and addresses actually are
- [iroh-ffi](https://github.com/n0-computer/iroh-ffi) — the Swift, Kotlin, Python, and Node bindings
