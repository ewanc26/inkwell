#!/usr/bin/env node
// Maintains the pinned copy of the canonical Standard.site Lexicons that
// Inkwell's iOS, Android, and shared KMP record models target, plus the
// conformance fixtures both platforms' tests decode.
//
// Inkwell hand-maintains three separate transcriptions of the same wire
// format (ATProtoKit `ATRecordProtocol` models in Swift, kotlinx.serialization
// data classes in Kotlin, and shared KMP types). Nothing previously tied any
// of them to the schemas Standard.site actually publishes, so they drifted --
// Android's DocumentRecord was missing `contributors` and `links` long after
// iOS had them. This is the layer that notices.
//
// Usage:
//   node tools/lexicons/update-standard-site.mjs [--check]
//     Offline. Verifies every checked-in schema still hashes to the CID
//     pinned in the manifest, that every fixture still hashes to its pinned
//     digest, and that every fixture validates against its pinned schema.
//     This is what CI runs: no network, so it can never flake or silently
//     absorb an upstream schema change.
//
//   node tools/lexicons/update-standard-site.mjs --update
//     Online. Re-resolves the Lexicon authority via DNS + DID + PDS, refetches
//     every schema, and rewrites the pinned schemas and manifest. Deliberate
//     and reviewable: it produces a diff a human approves, it is never wired
//     into CI, and fixtures are re-validated against the new schemas before
//     anything is written.
//
//   node tools/lexicons/update-standard-site.mjs --verify-remote
//     Online, read-only. Reports whether the pins have fallen behind what
//     Standard.site publishes, without touching the working tree.
//
//   node tools/lexicons/update-standard-site.mjs --pin-fixtures
//     Offline. For when a fixture was hand-authored or edited (adding a new
//     one, tweaking a field) without touching the Lexicon pins themselves:
//     regenerates the derived auth-permission-set fixture from the schemas
//     already on disk, re-validates every fixture against them, and rewrites
//     only the manifest's `fixtures` digests. Never touches `lexicons` or
//     `source` -- re-pinning those still requires network access via --update.
//
// Lexicon resolution follows the atproto spec: the NSID authority is the
// reversed domain (`site.standard.*` -> `standard.site`), whose `_lexicon`
// TXT record names the DID that publishes `com.atproto.lexicon.schema`
// records for that namespace.
//   https://atproto.com/specs/lexicon#lexicon-publication-and-resolution

import { readFileSync, writeFileSync, readdirSync, mkdirSync, existsSync } from "node:fs";
import { createHash } from "node:crypto";
import { resolveTxt } from "node:dns/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

import { cidForRecord, canonicalJson } from "./dag-cbor.mjs";

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "..", "..");
const lexiconDir = path.join(root, "lexicons", "standard-site");
const schemaDir = path.join(lexiconDir, "schemas");
const fixtureDir = path.join(lexiconDir, "fixtures");
const manifestPath = path.join(lexiconDir, "manifest.json");
const extensionsPath = path.join(lexiconDir, "inkwell-extensions.json");

// Derived rather than hand-authored: rebuilt from the pinned permission-set
// schemas so the platforms' OAuth scope constants can be asserted against the
// collections Standard.site actually grants, not against a transcription.
const DERIVED_AUTH_FIXTURE = "auth.permission-sets.json";

const NSID_AUTHORITY = "standard.site";
const SCHEMA_COLLECTION = "com.atproto.lexicon.schema";
const PLC_DIRECTORY = "https://plc.directory";

// The NSIDs Inkwell actually models. Adding one here and running --update is
// how a newly adopted Standard.site Lexicon enters the conformance layer.
const TRACKED_NSIDS = [
  "site.standard.publication",
  "site.standard.document",
  "site.standard.graph.subscription",
  "site.standard.graph.recommend",
  "site.standard.theme.basic",
  "site.standard.theme.color",
  "site.standard.authFull",
  "site.standard.authSocial",
];

const args = new Set(process.argv.slice(2));
const mode = args.has("--update")
  ? "update"
  : args.has("--verify-remote")
    ? "verify-remote"
    : args.has("--pin-fixtures")
      ? "pin-fixtures"
      : "check";

const problems = [];
const fail = (message) => problems.push(message);
const relative = (file) => path.relative(root, file);

// ── Resolution ────────────────────────────────────────────────────────────

async function resolveAuthorityDid(authority) {
  const records = await resolveTxt(`_lexicon.${authority}`);
  for (const chunks of records) {
    const value = chunks.join("");
    if (value.startsWith("did=")) return value.slice(4);
  }
  throw new Error(`No did= TXT record at _lexicon.${authority}`);
}

async function resolvePdsEndpoint(did) {
  if (!did.startsWith("did:plc:")) {
    throw new Error(`Only did:plc authorities are supported; got ${did}`);
  }
  const response = await fetch(`${PLC_DIRECTORY}/${did}`);
  if (!response.ok) throw new Error(`${PLC_DIRECTORY}/${did} returned ${response.status}`);
  const document = await response.json();
  const service = (document.service ?? []).find((entry) => entry.type === "AtprotoPersonalDataServer");
  if (!service?.serviceEndpoint) throw new Error(`No PDS service entry in the DID document for ${did}`);
  return service.serviceEndpoint.replace(/\/$/, "");
}

async function fetchSchemaRecord(pds, did, nsid) {
  const url = new URL(`${pds}/xrpc/com.atproto.repo.getRecord`);
  url.searchParams.set("repo", did);
  url.searchParams.set("collection", SCHEMA_COLLECTION);
  url.searchParams.set("rkey", nsid);
  const response = await fetch(url);
  if (!response.ok) throw new Error(`getRecord ${nsid} returned ${response.status}`);
  const record = await response.json();

  // The PDS-reported CID is only a claim until we can reproduce it from the
  // record body ourselves. If we can't, the pin would be unverifiable offline,
  // which is the whole point of pinning -- so treat it as fatal.
  const computed = cidForRecord(record.value);
  if (computed !== record.cid) {
    throw new Error(`CID mismatch for ${nsid}: PDS says ${record.cid}, recomputed ${computed}`);
  }
  return record;
}

// ── Fixture validation ────────────────────────────────────────────────────

/** Unwraps a `record` def to the object def that actually carries properties. */
const recordBody = (def) => (def?.type === "record" ? def.record : def);

function resolveDef(schemas, ref, selfNsid) {
  const [nsidPart, defPart] = ref.split("#");
  const nsid = nsidPart || selfNsid;
  const defName = defPart || "main";
  const schema = schemas[nsid];
  if (!schema) return null;
  return recordBody(schema.defs?.[defName]) ?? null;
}

function validateValue(value, spec, location, context) {
  const { schemas, nsid } = context;

  switch (spec.type) {
    case "string": {
      if (typeof value !== "string") return fail(`${location}: expected string, got ${typeof value}`);
      if (spec.maxLength !== undefined && Buffer.byteLength(value, "utf8") > spec.maxLength) {
        fail(`${location}: exceeds maxLength ${spec.maxLength} bytes`);
      }
      if (spec.format === "datetime" && !/^\d{4}-\d{2}-\d{2}T[\d:.]+(Z|[+-]\d{2}:\d{2})$/.test(value)) {
        fail(`${location}: "${value}" is not an RFC 3339 datetime`);
      }
      if (spec.format === "did" && !value.startsWith("did:")) {
        fail(`${location}: "${value}" is not a DID`);
      }
      if (spec.format === "at-uri" && !value.startsWith("at://")) {
        fail(`${location}: "${value}" is not an AT-URI`);
      }
      return;
    }
    case "integer":
      if (!Number.isInteger(value)) fail(`${location}: expected integer`);
      return;
    case "boolean":
      if (typeof value !== "boolean") fail(`${location}: expected boolean`);
      return;
    case "array": {
      if (!Array.isArray(value)) return fail(`${location}: expected array`);
      value.forEach((item, index) => validateValue(item, spec.items, `${location}[${index}]`, context));
      return;
    }
    case "blob": {
      if (value === null || typeof value !== "object") return fail(`${location}: expected a blob`);
      if (value.$type !== "blob") fail(`${location}: blob is missing $type "blob"`);
      if (typeof value.mimeType !== "string") fail(`${location}: blob is missing mimeType`);
      if (typeof value.size !== "number") fail(`${location}: blob is missing size`);
      if (typeof value.ref?.$link !== "string") fail(`${location}: blob is missing ref.$link`);
      if (Array.isArray(spec.accept)) {
        const accepted = spec.accept.some((pattern) =>
          pattern.endsWith("/*")
            ? value.mimeType?.startsWith(pattern.slice(0, -1))
            : value.mimeType === pattern,
        );
        if (!accepted) fail(`${location}: mimeType "${value.mimeType}" is outside accept ${spec.accept.join(", ")}`);
      }
      if (spec.maxSize !== undefined && value.size > spec.maxSize) {
        fail(`${location}: blob size ${value.size} exceeds maxSize ${spec.maxSize}`);
      }
      return;
    }
    case "ref": {
      // com.atproto.repo.strongRef isn't published as a schema record we pin,
      // so check it structurally rather than pretending we resolved it.
      if (spec.ref === "com.atproto.repo.strongRef") {
        if (typeof value?.uri !== "string" || typeof value?.cid !== "string") {
          fail(`${location}: expected a strongRef with uri and cid`);
        }
        return;
      }
      const def = resolveDef(schemas, spec.ref, nsid);
      if (!def) return; // Reference into a Lexicon we don't pin; nothing to assert.
      validateObject(value, def, location, context);
      return;
    }
    case "union": {
      if (value === null || typeof value !== "object") return fail(`${location}: expected an object`);
      // Open unions are the extension point Standard.site deliberately leaves
      // to each platform (content formats, link descriptors), so the one thing
      // every member must do is describe itself.
      if (typeof value.$type !== "string") return fail(`${location}: union member is missing $type`);
      if (spec.closed === true && !(spec.refs ?? []).includes(value.$type)) {
        return fail(`${location}: "${value.$type}" is not a member of this closed union`);
      }
      // If the member names a def we pin, hold it to that def.
      const def = resolveDef(schemas, value.$type, nsid);
      if (def) validateObject(value, def, location, context);
      return;
    }
    case "unknown":
      return;
    default:
      if (spec.type === "object") return validateObject(value, spec, location, context);
      fail(`${location}: unhandled Lexicon type "${spec.type}"`);
  }
}

function validateObject(value, def, location, context) {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    return fail(`${location}: expected an object`);
  }
  const properties = def.properties ?? {};
  for (const required of def.required ?? []) {
    if (value[required] === undefined) fail(`${location}: missing required property "${required}"`);
  }
  for (const [key, entry] of Object.entries(value)) {
    if (key === "$type") continue;
    const spec = properties[key];
    if (!spec) {
      // A property the Lexicon doesn't declare is drift unless it's a
      // documented extension -- see lexicons/standard-site/inkwell-extensions.json.
      if (context.extensions?.[context.nsid]?.[key]) continue;
      fail(
        `${location}: property "${key}" is not declared by the pinned ${context.nsid} Lexicon.\n` +
          `    If Standard.site added it, re-pin with --update. If publishers emit it without the\n` +
          `    Lexicon declaring it, document it in lexicons/standard-site/inkwell-extensions.json.`,
      );
      continue;
    }
    validateValue(entry, spec, `${location}.${key}`, context);
  }
}

function validateFixture(file, fixture, schemas, extensions) {
  const nsid = fixture.$type;
  if (typeof nsid !== "string") return fail(`${relative(file)}: fixture has no $type`);
  const schema = schemas[nsid];
  if (!schema) return fail(`${relative(file)}: $type "${nsid}" is not a pinned Lexicon`);
  const main = schema.defs?.main;
  if (!main) return fail(`${relative(file)}: pinned schema ${nsid} has no main def`);
  if (main.type === "permission-set") {
    return fail(`${relative(file)}: ${nsid} is a permission set, not a record type`);
  }
  validateObject(fixture, recordBody(main), path.basename(file, ".json"), { schemas, nsid, extensions });
}

/** The collections each pinned permission set grants, as the platforms must request them. */
function derivePermissionSets(schemas) {
  const sets = {};
  for (const [nsid, schema] of Object.entries(schemas)) {
    const main = schema.defs?.main;
    if (main?.type !== "permission-set") continue;
    const collections = new Set();
    for (const permission of main.permissions ?? []) {
      if (permission.resource !== "repo") continue;
      for (const collection of permission.collection ?? []) collections.add(collection);
    }
    sets[nsid] = {
      scope: `include:${nsid}`,
      title: main.title ?? null,
      detail: main.detail ?? null,
      collections: [...collections].sort(),
    };
  }
  return sets;
}

function renderPermissionSetFixture(schemas) {
  return canonicalJson({
    $comment:
      "Derived from the pinned site.standard.auth* permission-set Lexicons by " +
      "tools/lexicons/update-standard-site.mjs. Do not hand-edit -- --check rebuilds and compares it. " +
      "Both platforms' OAuth scope constants are asserted against this file.",
    permissionSets: derivePermissionSets(schemas),
  });
}

/** (Re)writes the derived permission-set fixture from the schemas currently in memory. */
function writeDerivedFixtures(schemas) {
  mkdirSync(fixtureDir, { recursive: true });
  writeFileSync(path.join(fixtureDir, DERIVED_AUTH_FIXTURE), renderPermissionSetFixture(schemas));
}

/**
 * Record properties Inkwell reads or preserves that the pinned Lexicons don't
 * declare -- see lexicons/standard-site/inkwell-extensions.json. Read-only:
 * this script never rewrites that file.
 */
function loadExtensions() {
  if (!existsSync(extensionsPath)) return {};
  const parsed = JSON.parse(readFileSync(extensionsPath, "utf8"));
  const { $comment, ...byNsid } = parsed;
  return byNsid;
}

// ── Manifest I/O ──────────────────────────────────────────────────────────

const sha256 = (text) => createHash("sha256").update(text, "utf8").digest("hex");

function readManifest() {
  if (!existsSync(manifestPath)) {
    throw new Error(`No pinned manifest at ${relative(manifestPath)} -- run with --update to create one.`);
  }
  return JSON.parse(readFileSync(manifestPath, "utf8"));
}

function listFixtureFiles() {
  if (!existsSync(fixtureDir)) return [];
  return readdirSync(fixtureDir).filter((name) => name.endsWith(".json")).sort();
}

function loadPinnedSchemas(manifest) {
  const schemas = {};
  for (const [nsid, pin] of Object.entries(manifest.lexicons)) {
    const file = path.join(schemaDir, `${nsid}.json`);
    if (!existsSync(file)) {
      fail(`${relative(file)} is missing but pinned in the manifest`);
      continue;
    }
    const text = readFileSync(file, "utf8");
    const value = JSON.parse(text);
    const computed = cidForRecord(value);
    if (computed !== pin.cid) {
      fail(
        `${relative(file)} no longer matches its pinned CID.\n` +
          `    pinned:     ${pin.cid}\n` +
          `    on disk:    ${computed}\n` +
          `    Run \`node tools/lexicons/update-standard-site.mjs --update\` to re-pin deliberately.`,
      );
    }
    if (text !== canonicalJson(value)) {
      fail(`${relative(file)} is not in canonical (DAG-CBOR key order) form; re-run --update`);
    }
    schemas[nsid] = value;
  }
  for (const name of readdirSync(schemaDir).filter((n) => n.endsWith(".json"))) {
    const nsid = name.slice(0, -5);
    if (!manifest.lexicons[nsid]) fail(`${relative(path.join(schemaDir, name))} is checked in but not pinned`);
  }
  return schemas;
}

function checkFixtures(manifest, schemas, extensions) {
  const onDisk = listFixtureFiles();
  const pinned = Object.keys(manifest.fixtures ?? {}).sort();

  for (const name of pinned) {
    if (!onDisk.includes(name)) fail(`fixtures/${name} is pinned but missing from disk`);
  }
  for (const name of onDisk) {
    if (!pinned.includes(name)) {
      fail(`fixtures/${name} is checked in but not listed in the manifest; re-run --update or --pin-fixtures`);
    }
  }

  for (const name of onDisk) {
    const file = path.join(fixtureDir, name);
    const text = readFileSync(file, "utf8");
    const pin = manifest.fixtures?.[name];
    if (pin && pin.sha256 !== sha256(text)) {
      fail(`fixtures/${name} was edited without re-pinning; re-run --update or --pin-fixtures`);
    }

    // The auth-permission-set fixture isn't a record instance of a pinned
    // Lexicon -- it's a derived summary of the auth* permission sets -- so it
    // doesn't go through validateFixture. Instead, check it's still exactly
    // what re-deriving it from the pinned schemas would produce.
    if (name === DERIVED_AUTH_FIXTURE) {
      const expected = renderPermissionSetFixture(schemas);
      if (text !== expected) {
        fail(`fixtures/${name} is stale -- re-run --update or --pin-fixtures to regenerate it from the pinned auth* Lexicons`);
      }
      continue;
    }

    let fixture;
    try {
      fixture = JSON.parse(text);
    } catch (error) {
      fail(`fixtures/${name}: ${error.message}`);
      continue;
    }
    validateFixture(file, fixture, schemas, extensions);
  }
}

function writeManifest(records, previous) {
  const lexicons = {};
  for (const nsid of TRACKED_NSIDS) {
    const record = records[nsid];
    lexicons[nsid] = { uri: record.uri, cid: record.cid };
  }
  const fixtures = {};
  for (const name of listFixtureFiles()) {
    fixtures[name] = { sha256: sha256(readFileSync(path.join(fixtureDir, name), "utf8")) };
  }
  const manifest = {
    $comment:
      "Pinned canonical Standard.site Lexicons that Inkwell's record models target. " +
      "Generated by tools/lexicons/update-standard-site.mjs -- do not hand-edit. " +
      "Each cid is the DAG-CBOR CIDv1 of the matching schemas/<nsid>.json body, " +
      "recomputed offline by --check so the pin is verifiable without network access.",
    source: {
      nsidAuthority: NSID_AUTHORITY,
      did: previous.did,
      collection: SCHEMA_COLLECTION,
      pds: previous.pds,
      resolution: `DNS TXT _lexicon.${NSID_AUTHORITY} -> DID -> PDS -> ${SCHEMA_COLLECTION}`,
    },
    resolvedAt: previous.resolvedAt,
    lexicons,
    fixtures,
  };
  writeFileSync(manifestPath, canonicalJson(manifest));
}

// ── Modes ─────────────────────────────────────────────────────────────────

async function fetchAll() {
  const did = await resolveAuthorityDid(NSID_AUTHORITY);
  const pds = await resolvePdsEndpoint(did);
  console.log(`Resolved ${NSID_AUTHORITY} -> ${did} -> ${pds}`);
  const records = {};
  for (const nsid of TRACKED_NSIDS) {
    records[nsid] = await fetchSchemaRecord(pds, did, nsid);
  }
  return { did, pds, records };
}

async function runUpdate() {
  const { did, pds, records } = await fetchAll();

  mkdirSync(schemaDir, { recursive: true });
  mkdirSync(fixtureDir, { recursive: true });

  const schemas = {};
  for (const nsid of TRACKED_NSIDS) {
    schemas[nsid] = records[nsid].value;
    writeFileSync(path.join(schemaDir, `${nsid}.json`), canonicalJson(records[nsid].value));
  }

  // Regenerate the derived auth-permission-set fixture from the freshly
  // fetched schemas before validating, so a change to what auth* grants is
  // reflected rather than checked against a stale derivation.
  writeDerivedFixtures(schemas);

  // Validate fixtures against the freshly fetched schemas *before* declaring
  // success, so an upstream change that invalidates a fixture surfaces as part
  // of the update rather than as a mystery CI failure later.
  checkFixtures(
    { fixtures: Object.fromEntries(listFixtureFiles().map((n) => [n, null])) },
    schemas,
    loadExtensions(),
  );
  if (problems.length > 0) return; // A fixture failed against the new schemas; don't pin over it.

  writeManifest(records, { did, pds, resolvedAt: new Date().toISOString() });
  console.log(`Pinned ${TRACKED_NSIDS.length} Lexicons into ${relative(manifestPath)}`);
}

async function runVerifyRemote() {
  const manifest = readManifest();
  const { records } = await fetchAll();
  for (const nsid of TRACKED_NSIDS) {
    const pinned = manifest.lexicons[nsid]?.cid;
    const live = records[nsid].cid;
    if (pinned !== live) {
      fail(`${nsid} has moved upstream: pinned ${pinned}, live ${live}`);
    } else {
      console.log(`  ${nsid} @ ${live}`);
    }
  }
}

function runCheck() {
  const manifest = readManifest();
  const schemas = loadPinnedSchemas(manifest);
  checkFixtures(manifest, schemas, loadExtensions());
  for (const nsid of TRACKED_NSIDS) {
    if (!manifest.lexicons[nsid]) fail(`${nsid} is tracked by the script but absent from the manifest`);
  }
  if (problems.length === 0) {
    console.log(
      `Standard.site conformance OK: ${Object.keys(manifest.lexicons).length} pinned Lexicons, ` +
        `${listFixtureFiles().length} fixtures (pinned ${manifest.resolvedAt}).`,
    );
  }
}

/** Rewrites only the manifest's `fixtures` digests, leaving `lexicons`/`source` untouched. */
function writeFixturePins(manifest) {
  const fixtures = {};
  for (const name of listFixtureFiles()) {
    fixtures[name] = { sha256: sha256(readFileSync(path.join(fixtureDir, name), "utf8")) };
  }
  writeFileSync(manifestPath, canonicalJson({ ...manifest, fixtures }));
}

function runPinFixtures() {
  const manifest = readManifest();
  const schemas = loadPinnedSchemas(manifest);
  if (problems.length > 0) return; // Pinned schemas themselves are broken; --update is what's needed.

  const extensions = loadExtensions();
  writeDerivedFixtures(schemas);

  // Validate content against the schemas, ignoring the manifest's stale
  // fixture digests -- that's exactly what this mode is here to refresh.
  checkFixtures(
    { fixtures: Object.fromEntries(listFixtureFiles().map((n) => [n, null])) },
    schemas,
    extensions,
  );
  if (problems.length > 0) return;

  writeFixturePins(manifest);
  console.log(`Pinned ${listFixtureFiles().length} fixtures into ${relative(manifestPath)} (Lexicon pins unchanged).`);
}

try {
  if (mode === "update") await runUpdate();
  else if (mode === "verify-remote") await runVerifyRemote();
  else if (mode === "pin-fixtures") runPinFixtures();
  else runCheck();
} catch (error) {
  console.error(`standard-site lexicons: ${error.message}`);
  process.exit(1);
}

if (problems.length > 0) {
  console.error(`\nStandard.site conformance failed (${problems.length}):\n`);
  for (const problem of problems) console.error(`  - ${problem}`);
  console.error("");
  process.exit(1);
}
