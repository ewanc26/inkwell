#!/usr/bin/env node
// Generates a lightweight SPDX-lite SBOM for Inkwell's iOS Swift Package
// Manager dependencies, parsed from the checked-in Package.resolved.
//
// There is no mature, low-friction "SPM SBOM" GitHub Action equivalent to
// anchore/sbom-action's Gradle/Java support (see .github/workflows/release.yml),
// and standing up a full CycloneDX/SPDX SPM tool is more tooling than this
// repo's dependency count (five pins) justifies. Package.resolved already is
// the exact resolved dependency graph -- same revisions Xcode fetches for a
// build -- so parsing it directly is both simpler and more accurate than
// re-deriving it from a build log.
//
// Usage:
//   node tools/release/generate-ios-sbom.mjs <path-to-Package.resolved> <output-path> [--version <string>]
//
// This intentionally does not attempt transitive dependency resolution
// beyond what Package.resolved itself records (SwiftPM already flattens the
// full transitive graph into one pins array), nor does it classify license
// info (SwiftPM does not expose license metadata in Package.resolved).

import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import path from "node:path";

const args = process.argv.slice(2);
const positional = args.filter((a) => !a.startsWith("--"));
const [resolvedPath, outputPath] = positional;

function flag(name) {
  const i = args.indexOf(`--${name}`);
  return i === -1 ? undefined : args[i + 1];
}

if (!resolvedPath || !outputPath) {
  console.error(
    "Usage: node tools/release/generate-ios-sbom.mjs <Package.resolved> <output.spdx.json> [--version <string>]",
  );
  process.exit(1);
}

const version = flag("version") ?? "unknown";

const resolved = JSON.parse(readFileSync(resolvedPath, "utf8"));
const pins = resolved.pins ?? [];

const now = new Date().toISOString().replace(/\.\d+Z$/, "Z");

function spdxId(identity) {
  // SPDX IDs must be alphanumeric plus "-" and ".".
  return `SPDXRef-Package-${identity.replace(/[^A-Za-z0-9.-]/g, "-")}`;
}

const packages = pins.map((pin) => {
  const state = pin.state ?? {};
  const versionInfo = state.version ?? state.branch ?? state.revision ?? "unspecified";
  const downloadLocation = state.revision
    ? `git+${pin.location}@${state.revision}`
    : (pin.location ?? "NOASSERTION");
  return {
    SPDXID: spdxId(pin.identity),
    name: pin.identity,
    versionInfo,
    downloadLocation,
    filesAnalyzed: false,
    licenseConcluded: "NOASSERTION",
    licenseDeclared: "NOASSERTION",
    copyrightText: "NOASSERTION",
    externalRefs: pin.location
      ? [
          {
            referenceCategory: "PACKAGE-MANAGER",
            referenceType: "purl",
            referenceLocator: `pkg:swift/${pin.location.replace(/^https?:\/\//, "").replace(/\.git$/, "")}@${state.revision ?? versionInfo}`,
          },
        ]
      : [],
    comment: pin.kind === "remoteSourceControl" ? undefined : `SwiftPM pin kind: ${pin.kind}`,
  };
});

const document = {
  spdxVersion: "SPDX-2.3",
  dataLicense: "CC0-1.0",
  SPDXID: "SPDXRef-DOCUMENT",
  name: `inkwell-ios-spm-dependencies-${version}`,
  documentNamespace: `https://inkwell.ewancroft.uk/sbom/ios/${version}-${Date.now()}`,
  creationInfo: {
    created: now,
    creators: ["Tool: inkwell-generate-ios-sbom.mjs"],
  },
  packages,
  // Every resolved package DEPENDS_ON the document's described package
  // (there's no single iOS app "package" in SwiftPM terms here -- Package.resolved
  // is consumed by the Inkwell Xcode project directly, not a Package.swift of
  // its own -- so relationships are recorded at the document level).
  relationships: packages.map((p) => ({
    spdxElementId: "SPDXRef-DOCUMENT",
    relationshipType: "DESCRIBES",
    relatedSpdxElement: p.SPDXID,
  })),
};

mkdirSync(path.dirname(path.resolve(outputPath)), { recursive: true });
writeFileSync(outputPath, JSON.stringify(document, null, 2) + "\n");
console.log(`Wrote ${packages.length} SwiftPM package(s) to ${outputPath}`);
