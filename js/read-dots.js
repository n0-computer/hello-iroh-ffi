// Headless reader for the hello-iroh dot demo.
//
// Run with no arguments to listen: it prints this peer's endpoint id, which
// you paste into the Connect field on another peer. Run with a remote
// endpoint id as the only argument to dial out instead.
//
// Either way it reads 8-byte position frames (f32 x, f32 y, little-endian)
// from the bi-directional stream and prints them to the console.

import { Endpoint, EndpointId, EndpointAddr } from "@number0/iroh";

const ALPN = Array.from(Buffer.from("iroh-helloiroh-dot/0"));
const FRAME_SIZE = 8;

function fmt(v) {
  return (v >= 0 ? "+" : "") + v.toFixed(3);
}

async function readFrames(bi, peer) {
  for (;;) {
    const frame = Buffer.from(await bi.recv.readExact(FRAME_SIZE));
    console.log(`[${peer}] x=${fmt(frame.readFloatLE(0))} y=${fmt(frame.readFloatLE(4))}`);
  }
}

async function listen(ep) {
  console.log("listening — paste this endpoint id into the other peer:");
  console.log(ep.id().toString());
  for (;;) {
    const incoming = await ep.acceptNext();
    if (incoming === null) return;
    let conn, bi;
    try {
      const accepting = await incoming.accept();
      if (!Buffer.from(await accepting.alpn()).equals(Buffer.from(ALPN))) continue;
      conn = await accepting.connect();
      bi = await conn.acceptBi();
    } catch (err) {
      console.log(`accept failed: ${err}`);
      continue;
    }
    const peer = conn.remoteId().fmtShort();
    console.log(`connected to ${peer}`);
    try {
      await readFrames(bi, peer);
    } catch {
      console.log(`${peer} disconnected`);
    }
  }
}

async function dial(ep, remoteStr) {
  const addr = new EndpointAddr(EndpointId.fromString(remoteStr.trim()));
  const conn = await ep.connect(addr, ALPN);
  const bi = await conn.openBi();
  // A QUIC stream is invisible to the peer until data is sent on it, so
  // send one position frame; this reader's "dot" sits at the origin.
  await bi.send.writeAll(Array.from(Buffer.alloc(FRAME_SIZE)));
  const peer = conn.remoteId().fmtShort();
  console.log(`connected to ${peer}`);
  await readFrames(bi, peer);
}

// Endpoint.bind applies the n0 preset (relay + discovery) by default.
const ep = await Endpoint.bind({ alpns: [ALPN] });
const remote = process.argv[2];
if (remote) {
  await dial(ep, remote);
} else {
  await listen(ep);
}
