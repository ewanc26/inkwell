#!/usr/bin/env node
// Issues an Inkwell customisation license key. Run this by hand after
// someone pays the one-off unlock fee via Ko-fi/GitHub Sponsors (see
// tools/license/README.md) -- there is no automated payment webhook.
//
// The private key never lives in this repo. Pass its path via
// --key or the INKWELL_LICENSE_PRIVATE_KEY env var.
//
// Usage:
//   node tools/license/generate-key.mjs --key ~/inkwell-license-signing/private-key.pem
//   INKWELL_LICENSE_PRIVATE_KEY=~/inkwell-license-signing/private-key.pem node tools/license/generate-key.mjs

import { createSign, createVerify } from "node:crypto";
import { readFileSync } from "node:fs";
import { homedir } from "node:os";
import path from "node:path";

// Must exactly match LicenseVerifier's LICENSE_MESSAGE constant on both
// platforms. Versioned so a future message change can't be satisfied by
// old keys, and old keys can be told apart from new ones if ever needed.
const LICENSE_MESSAGE = "inkwell-customisation-unlock-v1";

function resolvePath(p) {
  return p.startsWith("~") ? path.join(homedir(), p.slice(1)) : p;
}

const keyArgIndex = process.argv.indexOf("--key");
const keyPath = keyArgIndex !== -1 ? process.argv[keyArgIndex + 1] : process.env.INKWELL_LICENSE_PRIVATE_KEY;

if (!keyPath) {
  console.error("Usage: node tools/license/generate-key.mjs --key <path-to-private-key.pem>");
  console.error("   or: INKWELL_LICENSE_PRIVATE_KEY=<path> node tools/license/generate-key.mjs");
  process.exit(1);
}

const privateKeyPem = readFileSync(resolvePath(keyPath), "utf8");

const signer = createSign("SHA256");
signer.update(LICENSE_MESSAGE);
signer.end();
const signature = signer.sign(privateKeyPem); // DER-encoded ECDSA signature

const licenseKey = signature.toString("base64url");

console.log(licenseKey);
