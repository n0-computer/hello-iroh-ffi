# Hello Iroh FFI

This repositroy is a minimal presence demo over [iroh](https://github.com/n0-computer/iroh), implemented in parallel on iOS/macOS and Android. Each platform is a self-contained app that speaks the same wire format, so a Swift peer and a Kotlin peer can share a screen.

| Platform | Language | Status | Details |
|---|---|---|---|
| iOS + macOS | Swift / SwiftUI | working | [swift/README.md](swift/README.md) |
| Android | Kotlin / Jetpack Compose | working  | [kotlin-android/README.md](kotlin-android/README.md) |

Both implementations use [iroh-ffi](https://github.com/n0-computer/iroh-ffi) for the iroh bindings. Read more about language bindings at 

## How the demo works

Each peer controls one dot in a shared coordinate space. You move your dot by tilting the phone or dragging on the desktop, and you stream its position to the other peer at ~60 Hz over an iroh bi-directional stream. The peer renders your dot at the same coordinates next to its own, so both screens show the two dots tracking each other in real time.

The two peers are fully symmetric. There is no game, no score, and no authority role: each side sends its own position and renders the other's. Discovery is manual: copy your endpoint id, paste it on the other peer, tap Connect.

### Wire format

A single bi-directional stream carries one frame type, sent continuously by both peers:

| Frame | Size | Layout |
|---|---|---|
| Position | 8 B | `f32 x, f32 y` |

Both coordinates are in `[-1, 1]`, little-endian, and shared directly: the peer renders your dot at the position you send. With only one message type there is no tag byte, so the receive loop reads a fixed 8 bytes per frame.

ALPN: `iroh-helloiroh-dot/0`.

## Repo layout

```
iroh-dot/
├── README.md
├── swift/              iOS + macOS app (Swift / SwiftUI)
└── kotlin-android/     Android app (Kotlin / Jetpack Compose)
```

## Further reading

- [docs.iroh.computer](https://docs.iroh.computer) — iroh concepts and language guides
- [iroh-ffi](https://github.com/n0-computer/iroh-ffi) — Swift, Kotlin, Python, and Node bindings
