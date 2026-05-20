# iroh-pong — Kotlin (Android)

Android version of the iroh-pong demo. Two peers connect over an iroh bi-directional stream and play a round of Pong. Compose UI, Kotlin coroutines, and the [iroh-ffi](https://github.com/n0-computer/iroh-ffi) Kotlin bindings.

Wire-compatible with the Swift app in [../swift](../swift) — an Android peer can play against an iPhone or a Mac.

## Status

Builds clean against NDK r30 + iroh-ffi `feat-1-0`. Produces a ~62 MB debug APK with `libiroh_ffi.so` for all four ABIs (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`). Pending wall-clock runtime verification on a real Android device.

## Prerequisites

- **JDK 17** — `brew install openjdk@17`, or install Android Studio which bundles a JBR.
- **Android Studio Ladybug (2024.2) or newer**, with:
  - Android SDK platform 35
  - **NDK r26+** (Android Studio: *SDK Manager → SDK Tools → NDK (Side by side)*)
- **`cargo-ndk`** — `cargo install --version 3.5.4 cargo-ndk --locked`
- An Android device or emulator running Android 8.0 (API 26) or newer

## Setup

This project expects [`iroh-ffi`](https://github.com/n0-computer/iroh-ffi) checked out as a sibling of `iroh-pong` on the `feat-1-0` branch:

```
parent-dir/
├── iroh-pong/
│   └── kotlin-android/    ← this directory
└── iroh-ffi/              ← on branch feat-1-0
```

Until iroh-ffi publishes an Android-friendly Maven artifact, this project pulls its generated Kotlin sources and per-ABI `.so` files directly out of the sibling checkout via Gradle source-set inclusion. The `.so` files are produced by a one-time build:

```bash
cd ../../iroh-ffi
cargo make kotlin-android
```

This cross-compiles `libiroh_ffi.so` for `armeabi-v7a`, `arm64-v8a`, `x86`, and `x86_64` into `iroh-ffi/kotlin/lib/src/main/jniLibs/`, and regenerates the Kotlin bindings into `iroh-ffi/kotlin/lib/src/main/kotlin/`. First build takes 5–15 minutes; subsequent builds reuse cargo's cache.

## Build and run

Open `kotlin-android/` in Android Studio and let it finish the initial Gradle sync. From the command line:

```bash
./gradlew assembleDebug          # build the APK
./gradlew installDebug           # install on the attached device or running emulator
```

Or use *Run → Run 'app'* in Android Studio.

The app launches into a single screen: your endpoint id at the top with a Copy button and a gear (Settings), a text field for the peer's endpoint id with a Connect button, and the Pong field underneath. The endpoint id is persisted across launches, so the copy/paste happens once.

## Play the demo

1. Build and run on a phone (or two — or one phone and the Swift app on a Mac/iPhone).
2. On device A, tap **Copy** and send the id to device B.
3. On device B, paste into the **Peer endpoint id** field and tap **Connect**.
4. The peer that tapped Connect is the **ball authority** — it simulates ball physics and streams ball state.
5. Tilt the phone left/right to move your paddle. In the emulator, where there is no gravity sensor, drag horizontally on the playfield instead.
6. First to 7 wins. The score resets when a new session starts.

### Settings

The gear button opens a modal sheet with an entry for an iroh services API key (stored in SharedPreferences). A default key is bundled in source so telemetry comes up automatically on a fresh install; paste your own secret to override, or tap Clear to revert.

## Project layout

```
kotlin-android/
├── build.gradle.kts                 root plugin declarations
├── settings.gradle.kts              single :app module, no composite build
├── gradle.properties                Gradle + Android flags
└── app/
    ├── build.gradle.kts             Compose, source-set pull from iroh-ffi
    └── src/main/
        ├── AndroidManifest.xml      INTERNET + sensor declaration, portrait
        ├── java/computer/iroh/pong/
        │   ├── MainActivity.kt      Compose host
        │   ├── MainViewModel.kt     ties identity, motion, and peer together
        │   ├── identity/
        │   │   └── IdentityStore.kt persistent SecretKey + API secret
        │   ├── net/
        │   │   ├── IrohPeer.kt      bind, accept loop, connect, services client
        │   │   ├── PeerSession.kt   tagged frame send/recv on one bi-stream
        │   │   └── WireFormat.kt    ALPN + tag/byte layout, matches Swift
        │   ├── game/
        │   │   ├── PongGame.kt      paddles, ball, scores, physics, prediction
        │   │   ├── PongScene.kt     Compose Canvas rendering + drag input
        │   │   └── MotionSource.kt  gravity sensor + drag fallback
        │   └── ui/
        │       ├── EndpointScreen.kt
        │       ├── SettingsBottomSheet.kt
        │       └── theme/Theme.kt
        └── res/                     icons, strings, themes
```

## When iroh-ffi publishes

Drop the source-set lines in `app/build.gradle.kts` (the `sourceSets { ... }` block) and replace the JNA + coroutines `implementation(...)` lines with a single artifact dependency. The sibling checkout becomes optional.
