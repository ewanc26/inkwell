#!/usr/bin/env node
// Builds the small release-manifest.json attested and attached by
// .github/workflows/release.yml. This is the file that actually gets a
// GitHub Actions build-provenance attestation -- not the APK/IPA
// themselves, because those are still assembled and signed outside Actions
// (see RELEASE_PROCESS.md and Android/fdroid-repo/PROVENANCE.md /
// iOS/altstore/PROVENANCE.md). Attesting a CI rebuild of those binaries
// would describe different bytes than what's actually distributed, which
// is exactly the misleading-attestation failure mode #123 asks to avoid.
//
// Instead: this script hashes the *exact* APK/IPA bytes already uploaded to
// the GitHub Release (the same bytes a user downloads), records those
// hashes plus the release commit/tag/workflow-run identity in a manifest,
// and the workflow attests that manifest -- which this workflow genuinely
// did produce. A verifier who trusts the attestation on the manifest, then
// checks the manifest's sha256 against the file they downloaded, gets an
// equivalent guarantee without the workflow lying about who built the
// binary.
//
// Usage:
//   node tools/release/build-release-manifest.mjs \
//     --tag vX.Y.Z --assets-dir <dir> --sbom-dir <dir> --out <path>
//     [--commit <sha>] [--run-url <url>]

import { readFileSync, writeFileSync, readdirSync, existsSync } from "node:fs";
import { createHash } from "node:crypto";
import path from "node:path";

const args = process.argv.slice(2);
function opt(name, required = false) {
  const i = args.indexOf(`--${name}`);
  const value = i === -1 ? undefined : args[i + 1];
  if (required && !value) {
    console.error(`Missing required --${name}`);
    process.exit(1);
  }
  return value;
}

const tag = opt("tag", true);
const assetsDir = opt("assets-dir", true);
const sbomDir = opt("sbom-dir", true);
const outPath = opt("out", true);
const commit = opt("commit") ?? process.env.GITHUB_SHA ?? "unknown";
const runUrl =
  opt("run-url") ??
  (process.env.GITHUB_SERVER_URL && process.env.GITHUB_REPOSITORY && process.env.GITHUB_RUN_ID
    ? `${process.env.GITHUB_SERVER_URL}/${process.env.GITHUB_REPOSITORY}/actions/runs/${process.env.GITHUB_RUN_ID}`
    : "unknown");

const version = tag.replace(/^v/, "");

function sha256File(filePath) {
  return createHash("sha256").update(readFileSync(filePath)).digest("hex");
}

function listFiles(dir, extension) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir)
    .filter((f) => f.endsWith(extension))
    .sort()
    .map((name) => {
      const filePath = path.join(dir, name);
      return { name, sha256: sha256File(filePath), bytes: readFileSync(filePath).length };
    });
}

const apks = listFiles(assetsDir, ".apk");
const ipas = listFiles(assetsDir, ".ipa");
const sboms = [...listFiles(sbomDir, ".spdx.json")];

if (apks.length === 0 && ipas.length === 0) {
  console.error(
    `No .apk or .ipa assets found in ${assetsDir} for ${tag}. The GitHub Release for this tag ` +
      "should already carry the platform artifacts (published via tools/release/publish.mjs) " +
      "before this workflow runs.",
  );
  process.exit(1);
}

// Best-effort cross-check against the iOS AltStore manifest, which already
// records a sha256 for the newest published IPA (#119). Not a hard
// dependency -- older releases or Android-only releases won't have a
// matching entry -- but when both exist they must agree, since they're
// supposed to describe the same bytes.
function crossCheckIpa(ipa) {
  const sourcePath = path.resolve("iOS/altstore/source.json");
  if (!ipa || !existsSync(sourcePath)) return null;
  try {
    const source = JSON.parse(readFileSync(sourcePath, "utf8"));
    const entry = source.apps?.[0]?.versions?.find((v) => v.version === version);
    if (!entry) return { checked: false, reason: `no AltStore entry for version ${version}` };
    if (entry.sha256 !== ipa.sha256) {
      throw new Error(
        `IPA sha256 mismatch: release asset ${ipa.name} is ${ipa.sha256}, ` +
          `but iOS/altstore/source.json records ${entry.sha256} for ${version}`,
      );
    }
    return { checked: true, source: "iOS/altstore/source.json" };
  } catch (err) {
    if (err instanceof SyntaxError) return { checked: false, reason: "source.json unreadable" };
    throw err;
  }
}

const ipaCrossCheck = ipas[0] ? crossCheckIpa(ipas[0]) : null;

const manifest = {
  schema: "inkwell-release-manifest/1",
  version,
  tag,
  commit,
  workflowRun: runUrl,
  generatedAt: new Date().toISOString(),
  artifacts: {
    ...(apks[0] ? { apk: { name: apks[0].name, sha256: apks[0].sha256, bytes: apks[0].bytes } } : {}),
    ...(ipas[0]
      ? {
          ipa: {
            name: ipas[0].name,
            sha256: ipas[0].sha256,
            bytes: ipas[0].bytes,
            altstoreCrossCheck: ipaCrossCheck,
          },
        }
      : {}),
  },
  sboms: sboms.map((s) => ({ name: s.name, sha256: s.sha256 })),
  provenance: {
    binariesBuiltInActions: false,
    note:
      "The APK and IPA above are signed/assembled outside GitHub Actions (see " +
      "RELEASE_PROCESS.md, Android/fdroid-repo/PROVENANCE.md, iOS/altstore/PROVENANCE.md). " +
      "This manifest -- not the binaries -- carries the GitHub Actions build-provenance " +
      "attestation. Verify the manifest's attestation, then confirm a downloaded APK/IPA's " +
      "sha256 matches the corresponding entry above.",
  },
};

writeFileSync(outPath, JSON.stringify(manifest, null, 2) + "\n");
console.log(`Wrote release manifest for ${tag} to ${outPath}`);
console.log(JSON.stringify(manifest, null, 2));
