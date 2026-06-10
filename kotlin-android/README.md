# iroh-dot — Kotlin (Android)

Android version of the iroh-dot demo. Two peers connect over an iroh bi-directional stream, and each controls one dot in a shared coordinate space. Compose UI, Kotlin coroutines, and the [iroh-ffi](https://github.com/n0-computer/iroh-ffi) Kotlin bindings.

Wire-compatible with the Swift app in [../swift](../swift): an Android peer shares a screen with an iPhone or a Mac.

## Status

Working end-to-end on a headless Android 14 arm64 emulator: bind an Endpoint, load persistent identity, activate iroh-services telemetry, render the dot field. Cross-platform play against the Swift app should follow from the shared wire format.

Requires iroh-ffi's [`feat-1-0-android-context`](https://github.com/n0-computer/iroh-ffi/tree/feat-1-0-android-context) branch, which adds an Android JNI initialization hook so iroh's DNS resolver can read system DNS via `LinkProperties`. The app calls `IrohAndroid.installAndroidContext(applicationContext)` once at startup — see `MainViewModel.kt`. When that branch lands on `feat-1-0` (or in a published Maven artifact), this requirement goes away.

## Prerequisites

- **JDK 17** — `brew install openjdk@17`, or install Android Studio which bundles a JBR.
- **Android Studio Ladybug (2024.2) or newer**, with:
  - Android SDK platform 35
  - **NDK r26+** (Android Studio: *SDK Manager → SDK Tools → NDK (Side by side)*)
- **`cargo-ndk`** — `cargo install --version 3.5.4 cargo-ndk --locked`
- An Android device or emulator running Android 8.0 (API 26) or newer

## Setup

This project expects [`iroh-ffi`](https://github.com/n0-computer/iroh-ffi) checked out as a sibling of `iroh-dot` on the `feat-1-0-android-context` branch:

```
parent-dir/
├── iroh-dot/
│   └── kotlin-android/    ← this directory
└── iroh-ffi/              ← on branch feat-1-0-android-context
```

```bash
git clone --branch feat-1-0-android-context https://github.com/n0-computer/iroh-ffi.git
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

The app launches into a single screen: your endpoint id at the top with a Copy button, a text field for the peer's endpoint id with a Connect button, and the dot field underneath. The endpoint id is persisted across launches, so the copy/paste happens once.

## Play the demo

1. Build and run on a phone (or two, or one phone and the Swift app on a Mac/iPhone).
2. On device A, tap **Copy** and send the id to device B.
3. On device B, paste into the **Peer endpoint id** field and tap **Connect**.
4. Tilt the phone to move your dot: it rolls toward the lowered edge, like a ball on a tray. In the emulator, where there is no gravity sensor, drag on the field instead.
5. Both dots appear in the same coordinate space, tinted by endpoint id, so you can watch the peer's dot track yours as either of you moves.

### Telemetry (optional)

The app starts an iroh services client at boot so its metrics can show up in a [services.iroh.computer](https://services.iroh.computer) dashboard. The API key in `IrohPeer.kt` is a placeholder; paste your own to enable it. With the placeholder left in place the client fails to start and the demo works normally without telemetry.

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
        ├── java/computer/iroh/dot/
        │   ├── MainActivity.kt      Compose host
        │   ├── MainViewModel.kt     ties identity, motion, and peer together
        │   ├── identity/
        │   │   └── IdentityStore.kt persistent SecretKey + API secret
        │   ├── net/
        │   │   ├── IrohPeer.kt      bind, accept loop, connect, services client
        │   │   ├── PeerSession.kt   position send/recv on one bi-stream
        │   │   └── WireFormat.kt    ALPN + position frame layout, matches Swift
        │   ├── game/
        │   │   ├── DotGame.kt       my dot + peer's dot positions
        │   │   ├── DotScene.kt      Compose Canvas rendering + drag input
        │   │   └── MotionSource.kt  gravity sensor + drag fallback
        │   └── ui/
        │       ├── EndpointScreen.kt
        │       └── theme/Theme.kt
        └── res/                     icons, strings, themes
```

## When iroh-ffi publishes

Drop the source-set lines in `app/build.gradle.kts` (the `sourceSets { ... }` block) and replace the JNA + coroutines `implementation(...)` lines with a single artifact dependency. The sibling checkout becomes optional.
