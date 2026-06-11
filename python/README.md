# Python console reader

A headless peer for the dot demo: it speaks the same wire format
(ALPN `iroh-helloiroh-dot/0`, 8-byte `f32 x, f32 y` frames) but just prints
every received position to the console instead of rendering a dot.

Uses the [iroh Python bindings](https://docs.iroh.computer/languages/python).

## Setup

```bash
uv venv --python 3.12 .venv
uv pip install --python .venv/bin/python --prerelease=allow 'iroh==1.0.0rc1'
```

## Run

Listen and print your endpoint id (paste it into the Connect field on a
phone/desktop peer):

```bash
.venv/bin/python read_dots.py
```

Or dial out to a peer's endpoint id:

```bash
.venv/bin/python read_dots.py <endpoint-id>
```

Either way, incoming frames print as:

```
[78a2b96cdb] x=+0.412 y=-0.083
```

When dialing, the reader sends a single `(0, 0)` frame so the QUIC stream
becomes visible to the peer — your "dot" sits at the origin on their screen.
