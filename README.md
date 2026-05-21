# iroh-pong

Two-player Pong over [iroh](https://github.com/n0-computer/iroh), implemented in parallel on iOS/macOS and Android. Each platform is a self-contained app that speaks the same wire format, so a Swift peer and a Kotlin peer can play each other.

| Platform | Language | Status | Details |
|---|---|---|---|
| iOS + macOS | Swift / SwiftUI | working | [swift/README.md](swift/README.md) |
| Android | Kotlin / Jetpack Compose | working (against iroh-ffi `feat-1-0-android-context`) | [kotlin-android/README.md](kotlin-android/README.md) |

Both implementations use [iroh-ffi](https://github.com/n0-computer/iroh-ffi) for the iroh bindings.

## How the demo works

Two peers connect over an iroh bi-directional stream and exchange paddle positions and ball state at ~60 Hz. Paddles move with device tilt on phones and with a mouse drag on the desktop builds. Discovery is manual: copy your endpoint id, paste it on the other peer, tap Connect.

The peer that taps Connect is the ball authority — it simulates ball physics and streams ball state. The other peer sends paddle x only. First to 7 wins.

### Wire format

A single bi-directional stream carries two tagged frame types:

| Tag | Frame | Size | Sender |
|---|---|---|---|
| `0` | Paddle: `f32 x` | 5 B | both peers |
| `1` | Ball: `f32 x, f32 y, f32 vx, f32 vy, u16 myScore, u16 theirScore` | 21 B | authority only |

All coordinates are in `[-1, 1]`. Each peer renders itself on the bottom of its own screen, so y is flipped on receive; x is shared. Velocity rides along with the ball frame so the non-authority can extrapolate between snapshots.

ALPN: `iroh-helloiroh-pong/0`.

## Repo layout

```
iroh-pong/
├── README.md
├── swift/              iOS + macOS app (Swift / SwiftUI)
└── kotlin-android/     Android app (Kotlin / Jetpack Compose)
```

## Further reading

- [docs.iroh.computer](https://docs.iroh.computer) — iroh concepts and language guides
- [iroh-ffi](https://github.com/n0-computer/iroh-ffi) — Swift, Kotlin, Python, and Node bindings
