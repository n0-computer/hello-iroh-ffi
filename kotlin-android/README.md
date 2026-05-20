# iroh-pong — Kotlin (Android)

Android implementation of the iroh-pong demo. Not started yet.

The plan is a Jetpack Compose app using the Kotlin bindings from [iroh-ffi](https://github.com/n0-computer/iroh-ffi), interoperable with the Swift app in [../swift](../swift). See the top-level [README](../README.md) for the wire format and gameplay.

Tracking notes:

- Paddle input: device tilt via `SensorManager` (`TYPE_GRAVITY`).
- Discovery: manual endpoint id copy/paste, matching the Swift app.
- ALPN and frame layout: identical to the Swift implementation so peers across platforms can play each other.
