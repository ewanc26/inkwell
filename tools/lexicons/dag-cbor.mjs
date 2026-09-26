// Minimal, dependency-free DAG-CBOR encoder + CIDv1 computation, sufficient
// for the subset of IPLD that AT Protocol Lexicon schema records use (maps,
// arrays, strings, integers, booleans, null).
//
// Why hand-roll this rather than pull in `@ipld/dag-cbor`: this repo has no
// Node dependency manifest at the root (the only npm project is `website/`),
// and the conformance check has to run in CI without a package install step.
// The encoder is small enough to be auditable and is self-tested against the
// live PDS every time `update-standard-site.mjs --update` runs: a pinned CID
// that this code cannot reproduce from the record body is a hard error.
//
// Spec references:
//   - DAG-CBOR: https://ipld.io/specs/codecs/dag-cbor/spec/
//   - CID:      https://github.com/multiformats/cid
//   - atproto data model: https://atproto.com/specs/data-model

import { createHash } from "node:crypto";

const DAG_CBOR_CODEC = 0x71;
const SHA2_256 = 0x12;

function encodeHead(major, argument) {
  const head = major << 5;
  if (argument < 24) return Buffer.from([head | argument]);
  if (argument < 0x100) return Buffer.from([head | 24, argument]);
  if (argument < 0x10000) {
    const buffer = Buffer.alloc(3);
    buffer[0] = head | 25;
    buffer.writeUInt16BE(argument, 1);
    return buffer;
  }
  if (argument < 0x100000000) {
    const buffer = Buffer.alloc(5);
    buffer[0] = head | 26;
    buffer.writeUInt32BE(argument, 1);
    return buffer;
  }
  const buffer = Buffer.alloc(9);
  buffer[0] = head | 27;
  buffer.writeBigUInt64BE(BigInt(argument), 1);
  return buffer;
}

// DAG-CBOR orders map keys length-first, then bytewise on the UTF-8 encoding.
// (This is the IPLD rule, which differs from RFC 8949's plain bytewise
// ordering -- getting it wrong yields a valid-looking but wrong CID.)
function compareMapKeys(a, b) {
  const left = Buffer.from(a, "utf8");
  const right = Buffer.from(b, "utf8");
  if (left.length !== right.length) return left.length - right.length;
  return Buffer.compare(left, right);
}

export function encodeDagCbor(value) {
  if (value === null) return Buffer.from([0xf6]);
  if (value === true) return Buffer.from([0xf5]);
  if (value === false) return Buffer.from([0xf4]);

  if (typeof value === "number") {
    if (!Number.isInteger(value)) {
      // DAG-CBOR mandates float64 for non-integers. No Lexicon schema field
      // uses one today; refuse rather than emit something unverified.
      throw new Error(`Refusing to DAG-CBOR encode non-integer number ${value}`);
    }
    return value >= 0 ? encodeHead(0, value) : encodeHead(1, -value - 1);
  }

  if (typeof value === "string") {
    const bytes = Buffer.from(value, "utf8");
    return Buffer.concat([encodeHead(3, bytes.length), bytes]);
  }

  if (Array.isArray(value)) {
    return Buffer.concat([encodeHead(4, value.length), ...value.map(encodeDagCbor)]);
  }

  if (typeof value === "object") {
    // `{"$link": "..."}` is DAG-JSON's spelling of a CID link, and `{"$bytes":
    // "..."}` its spelling of a byte string; both encode as dedicated CBOR
    // types rather than maps. Lexicon schema records contain neither, so
    // rather than implement them half-right, fail loudly if one turns up.
    const keys = Object.keys(value);
    if (keys.length === 1 && (keys[0] === "$link" || keys[0] === "$bytes")) {
      throw new Error(`DAG-JSON ${keys[0]} encoding is not implemented`);
    }
    const sorted = keys.sort(compareMapKeys);
    const parts = [encodeHead(5, sorted.length)];
    for (const key of sorted) {
      parts.push(encodeDagCbor(key), encodeDagCbor(value[key]));
    }
    return Buffer.concat(parts);
  }

  throw new Error(`Cannot DAG-CBOR encode value of type ${typeof value}`);
}

const BASE32_ALPHABET = "abcdefghijklmnopqrstuvwxyz234567";

function base32Lower(bytes) {
  let bits = 0;
  let accumulator = 0;
  let out = "";
  for (const byte of bytes) {
    accumulator = (accumulator << 8) | byte;
    bits += 8;
    while (bits >= 5) {
      bits -= 5;
      out += BASE32_ALPHABET[(accumulator >> bits) & 31];
    }
  }
  if (bits > 0) out += BASE32_ALPHABET[(accumulator << (5 - bits)) & 31];
  return out;
}

function varint(value) {
  const bytes = [];
  let remaining = value;
  while (remaining >= 0x80) {
    bytes.push((remaining & 0x7f) | 0x80);
    remaining >>>= 7;
  }
  bytes.push(remaining);
  return Buffer.from(bytes);
}

/** Computes the CIDv1 (dag-cbor, sha2-256, base32) of a decoded JSON value. */
export function cidForRecord(value) {
  const digest = createHash("sha256").update(encodeDagCbor(value)).digest();
  const bytes = Buffer.concat([
    varint(1),
    varint(DAG_CBOR_CODEC),
    varint(SHA2_256),
    varint(digest.length),
    digest,
  ]);
  return `b${base32Lower(bytes)}`;
}

/** Re-serializes a value with DAG-CBOR's key ordering, for stable on-disk diffs. */
export function canonicalJson(value, indent = 2) {
  const order = (node) => {
    if (Array.isArray(node)) return node.map(order);
    if (node && typeof node === "object") {
      const out = {};
      for (const key of Object.keys(node).sort(compareMapKeys)) out[key] = order(node[key]);
      return out;
    }
    return node;
  };
  return `${JSON.stringify(order(value), null, indent)}\n`;
}
