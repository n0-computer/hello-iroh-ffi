#!/usr/bin/env python3
"""Headless reader for the hello-iroh dot demo.

Run with no arguments to listen: it prints this peer's endpoint id, which you
paste into the Connect field on another peer. Run with a remote endpoint id as
the only argument to dial out instead.

Either way it reads 8-byte position frames (f32 x, f32 y, little-endian) from
the bi-directional stream and prints them to the console.
"""

import asyncio
import struct
import sys

import iroh

ALPN = b"iroh-helloiroh-dot/0"
FRAME_SIZE = 8


async def read_frames(bi, peer):
    recv = bi.recv()
    while True:
        frame = await recv.read_exact(FRAME_SIZE)
        x, y = struct.unpack("<ff", frame)
        print(f"[{peer}] x={x:+.3f} y={y:+.3f}", flush=True)


async def listen(ep):
    print("listening — paste this endpoint id into the other peer:", flush=True)
    print(str(ep.id()), flush=True)
    while True:
        incoming = await ep.accept_next()
        if incoming is None:
            return
        try:
            accepting = await incoming.accept()
            if await accepting.alpn() != ALPN:
                continue
            conn = await accepting.connect()
            bi = await conn.accept_bi()
        except iroh.IrohError as e:
            print(f"accept failed: {e}", flush=True)
            continue
        peer = conn.remote_id().fmt_short()
        print(f"connected to {peer}", flush=True)
        try:
            await read_frames(bi, peer)
        except iroh.IrohError:
            print(f"{peer} disconnected", flush=True)


async def dial(ep, remote_str):
    remote = iroh.EndpointId.from_string(remote_str.strip())
    addr = iroh.EndpointAddr(remote, None, [])
    conn = await ep.connect(addr, ALPN)
    bi = await conn.open_bi()
    # A QUIC stream is invisible to the peer until data is sent on it, so
    # send one position frame; this reader's "dot" sits at the origin.
    await bi.send().write_all(struct.pack("<ff", 0.0, 0.0))
    peer = conn.remote_id().fmt_short()
    print(f"connected to {peer}", flush=True)
    await read_frames(bi, peer)


async def main():
    iroh.iroh_ffi.uniffi_set_event_loop(asyncio.get_running_loop())
    ep = await iroh.Endpoint.bind(
        iroh.EndpointOptions(preset=iroh.preset_n0(), alpns=[ALPN])
    )
    if len(sys.argv) > 1:
        await dial(ep, sys.argv[1])
    else:
        await listen(ep)


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
