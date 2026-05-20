# iroh-pong — Kotlin (Android)

Android version of the iroh-pong demo. Two peers connect over an iroh bi-directional stream and play a round of Pong. Compose UI, Kotlin coroutines, and the [iroh-ffi](https://github.com/n0-computer/iroh-ffi) Kotlin bindings.

Wire-compatible with the Swift app in [../swift](../swift) — an Android peer can play against an iPhone or a Mac.

## Status

Stage 1 of three: scaffolds the Compose app, binds an iroh `Endpoint`, and shows the endpoint id. No gameplay yet. See `../plans/kotlin-android.md` for the staged plan.

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

The app launches into a single screen that shows your iroh endpoint id with a Copy button. The endpoint is bound fresh on every launch (persistent identity comes in stage 3).

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
        │   ├── MainViewModel.kt     binds the Endpoint, exposes state
        │   └── ui/
        │       ├── EndpointScreen.kt
        │       └── theme/Theme.kt
        └── res/                     icons, strings, themes
```

## When iroh-ffi publishes

Drop the source-set lines in `app/build.gradle.kts` (the `sourceSets { ... }` block) and replace the JNA + coroutines `implementation(...)` lines with a single artifact dependency. The sibling checkout becomes optional.
