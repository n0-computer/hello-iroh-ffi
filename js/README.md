# JavaScript (Node) console reader

A headless peer for the dot demo: it speaks the same wire format
(ALPN `iroh-helloiroh-dot/0`, 8-byte `f32 x, f32 y` frames) but just prints
every received position to the console instead of rendering a dot.

Uses the [iroh JavaScript bindings](https://docs.iroh.computer/languages/javascript)
(`@number0/iroh`). Requires Node 20.3+.

## Setup

```bash
npm install
```

## Run

Listen and print your endpoint id (paste it into the Connect field on a
phone/desktop peer):

```bash
node read-dots.js
```

Or dial out to a peer's endpoint id:

```bash
node read-dots.js <endpoint-id>
```

Either way, incoming frames print as:

```
[8afb782ee0] x=+0.412 y=-0.083
```

When dialing, the reader sends a single `(0, 0)` frame so the QUIC stream
becomes visible to the peer — your "dot" sits at the origin on their screen.
