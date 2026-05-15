# HelloIroh

A small iOS + macOS demo built on [iroh](https://github.com/n0-computer/iroh) via the [iroh-ffi](https://github.com/n0-computer/iroh-ffi) Swift bindings.

Each peer renders two balls — its own and the remote peer's — connected over an iroh bi-directional stream. On iOS, the local ball follows device gravity via CoreMotion. On macOS, you drag it with the mouse. Discovery is manual: copy your endpoint id, paste it on the other peer, tap Connect.

The endpoint id is persisted across launches, so the copy/paste happens once and the demo keeps working session over session.

## Prerequisites

- macOS with [Xcode](https://developer.apple.com/xcode/) 16 or newer
- [Rust](https://www.rust-lang.org/tools/install) with [`cargo-make`](https://crates.io/crates/cargo-make): `cargo install cargo-make`
- A free Apple Developer account if you want to run on a physical iPhone

> While iroh-ffi is still on a 1.0 release candidate, the published Swift binary may not match the latest source. The setup below builds the xcframework locally to guarantee they line up. Once iroh-ffi ships a stable release, the `cargo make` step becomes optional — Xcode can just resolve the Swift Package from GitHub.

## Setup

This project expects `iroh-ffi` checked out as a sibling of `helloiroh`:

```
parent-dir/
├── helloiroh/
│   └── HelloIroh/      ← this repo
└── iroh-ffi/
```

Clone iroh-ffi and install the Apple Rust targets:

```bash
git clone https://github.com/n0-computer/iroh-ffi
cd iroh-ffi
rustup target add \
  aarch64-apple-ios \
  aarch64-apple-ios-sim \
  x86_64-apple-ios \
  aarch64-apple-darwin
```

Build the xcframework (first build takes 5–15 minutes; later builds reuse cargo's cache):

```bash
cargo make swift-xcframework
```

This produces `iroh-ffi/IrohLib/artifacts/Iroh.xcframework`, which the Xcode project consumes as a local Swift Package.

## Build and run

Open `HelloIroh.xcodeproj` in Xcode. The project has a single multiplatform SwiftUI target.

- **macOS**: select **My Mac** and Run.
- **iOS Simulator**: pick any iPhone simulator destination and Run.
- **iOS device**: select your device, signed with your team. The first time, trust the developer certificate under **Settings → General → VPN & Device Management**.

The app launches into a small UI: your full endpoint id at the top with a Copy button, a text field for the peer's endpoint id with a Connect button, and a canvas underneath where the two balls live.

## Use the demo

1. Build and run on two devices (e.g. your Mac and an iPhone, or two iPhones).
2. On device A, tap **Copy** and send the id to device B (Messages, AirDrop, whatever).
3. On device B, paste into the **Peer endpoint id** field and tap **Connect**.
4. Tilt the iPhone — its ball drifts in the direction of gravity, and the remote ball appears in the same position on the Mac (and vice versa).
5. On the Mac, drag the ball with the mouse to move it; the iPhone sees it too.

Both peers stream their position at ~30 Hz over a single bi-directional iroh stream. The wire format is an 8-byte frame containing two little-endian `Float32`s (x, y) in `[-1, 1]`.

## Project layout

```
HelloIroh/
├── HelloIroh/
│   ├── HelloIrohApp.swift     entry point
│   ├── ContentView.swift      assembles the UI
│   ├── IdentityStore.swift    persists the secret key in UserDefaults
│   ├── IrohPeer.swift         binds the Endpoint, runs the accept loop
│   ├── PeerSession.swift      one bi-stream's send/recv tasks
│   ├── MotionSource.swift     iOS gravity / macOS drag
│   ├── BallScene.swift        canvas rendering
│   ├── WireFormat.swift       ALPN + 8-byte frame encode/decode
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
