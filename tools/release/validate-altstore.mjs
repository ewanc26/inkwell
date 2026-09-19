#!/usr/bin/env node

import { readFileSync } from "node:fs";
import { createHash } from "node:crypto";

const paths = ["iOS/altstore/source.json", "website/static/altstore/source.json"];
const manifests = paths.map((path) => JSON.parse(readFileSync(path, "utf8")));
const [source, mirror] = manifests;
if (JSON.stringify(source) !== JSON.stringify(mirror)) {
  throw new Error("AltStore source manifests are not byte-for-byte identical");
}

const app = source.apps?.[0];
const latest = app?.versions?.[0];
if (!latest?.downloadURL || !latest?.size || !/^[a-f0-9]{64}$/.test(latest.sha256 ?? "")) {
  throw new Error("The newest AltStore version must include downloadURL, size, and a SHA-256 digest");
}
if (app.size !== latest.size || app.sha256 !== latest.sha256 || app.downloadURL !== latest.downloadURL) {
  throw new Error("AltStore app-level metadata does not match its newest version entry");
}

if (process.argv.includes("--hosted")) {
  const response = await fetch(latest.downloadURL);
  if (!response.ok) throw new Error(`Unable to fetch hosted IPA: HTTP ${response.status}`);
  const bytes = Buffer.from(await response.arrayBuffer());
  const digest = createHash("sha256").update(bytes).digest("hex");
  if (bytes.length !== latest.size || digest !== latest.sha256) {
    throw new Error(`Hosted IPA mismatch: size=${bytes.length}, sha256=${digest}`);
  }
}

console.log(`AltStore manifest valid: ${latest.version} build ${latest.buildVersion}, ${latest.size} bytes, sha256 ${latest.sha256}`);
