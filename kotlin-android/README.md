# Hello Iroh FFI — Kotlin (Android)

Android version of the Hello Iroh FFI demo. Two peers connect over an iroh bi-directional stream, and each controls one dot in a shared coordinate space. Compose UI, Kotlin coroutines, and the [iroh-ffi](https://github.com/n0-computer/iroh-ffi) Kotlin bindings from Maven Central.

Wire-compatible with the Swift app in [../swift](../swift): an Android peer shares a screen with an iPhone or a Mac.

## Prerequisites

- **JDK 17** — `brew install openjdk@17`, or install Android Studio which bundles a JBR.
- **Android Studio Ladybug (2024.2) or newer**, with Android SDK platform 35
- An Android device or emulator running Android 8.0 (API 26) or newer

That's it. The app depends on [`computer.iroh:iroh`](https://central.sonatype.com/artifact/computer.iroh/iroh) from Maven Central, which bundles the native `libiroh_ffi.so` for every Android ABI — no NDK, no Rust toolchain, no iroh-ffi checkout.

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
6. The line under the status shows the connection's live paths — direct vs relay, address, RTT, with `*` on the path carrying data. Watch it flip from relay to direct as iroh hole-punches.

### Telemetry (optional)

The app starts an iroh services client at boot so its metrics can show up in a [services.iroh.computer](https://services.iroh.computer) dashboard. The API key in `IrohPeer.kt` is a placeholder; paste your own to enable it. With the placeholder left in place the client fails to start and the demo works normally without telemetry.

## Project layout

```
kotlin-android/
├── build.gradle.kts                 root plugin declarations
├── settings.gradle.kts              single :app module, no composite build
├── gradle.properties                Gradle + Android flags
└── app/
    ├── build.gradle.kts             Compose + the iroh artifact from Maven Central
    └── src/main/
        ├── AndroidManifest.xml      INTERNET + sensor declaration, portrait
        ├── java/computer/iroh/dot/
        │   ├── MainActivity.kt      Compose host
        │   ├── MainViewModel.kt     ties identity, motion, and peer together
        │   ├── identity/
        │   │   └── IdentityStore.kt persistent SecretKey
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

## Configuration quirks worth knowing

These are wired up already, but worth knowing if you're starting a similar project from scratch:

- **`IrohAndroid.installAndroidContext(applicationContext)` once at startup.** iroh's DNS resolver reads system DNS via `LinkProperties`, which needs the process's `JavaVM` and a `Context` installed before the first `Endpoint` is constructed. See `MainViewModel.kt`.
- **JNA comes from the `@aar` variant.** The iroh artifact declares plain-jar JNA transitively, but Android needs the `@aar` variant, which bundles `libjnidispatch.so` per ABI. `app/build.gradle.kts` excludes the transitive jar and declares the `@aar` explicitly; keeping both causes a duplicate-class error at packaging time.
- **Kotlin 2.2+.** The published iroh artifact carries Kotlin 2.2 metadata, which a 2.0 compiler can't read ("Module was compiled with an incompatible version of Kotlin").
- **No extra permissions.** Everything the app does — QUIC to peers and relays, DNS/pkarr lookups — is covered by `INTERNET`. Notably no `NSLocalNetworkUsageDescription`-style local-network permission exists on Android, and no multicast permissions are needed since the demo does no mDNS discovery.

## Further reading

- [docs.iroh.computer/languages/kotlin](https://docs.iroh.computer/languages/kotlin) — the Kotlin setup guide
- [docs.iroh.computer/concepts/endpoints](https://docs.iroh.computer/concepts/endpoints) — what endpoints, ids, and addresses actually are
- [iroh-ffi](https://github.com/n0-computer/iroh-ffi) — the Swift, Kotlin, Python, and Node bindings
