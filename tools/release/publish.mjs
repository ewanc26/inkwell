#!/usr/bin/env node
// Publishes an Inkwell release: derives the version from source (the same
// build files tools/legal/render.mjs already trusts, not a hand-typed
// argument that can drift), then closes every distribution channel for
// that version -- GitHub Releases, and for Android also the self-hosted
// F-Droid repo. iOS archiving/export needs an interactive Xcode keychain
// unlock and can't run headlessly here, so `ios` takes an already-exported
// .ipa rather than building one.
//
// Usage:
//   node tools/release/publish.mjs status
//     Read-only. Compares the version each platform's source declares
//     against what's actually published on every channel, and prints the
//     gaps -- this is exactly the check that found android-v2.1.0's
//     missing GitHub release and F-Droid sitting a version behind.
//
//   node tools/release/publish.mjs android [--yes] [--push] [--notes-file <path>]
//     Builds a signed release APK, publishes it to the self-hosted F-Droid
//     repo (updates metadata, regenerates the signed index, mirrors it into
//     website/static/fdroid/), and publishes a GitHub release with the APK
//     attached.
//
//   node tools/release/publish.mjs ios --ipa <path> [--yes] [--push] [--notes-file <path>]
//     Publishes an already-exported .ipa to the AltStore source (updates
//     source.json, copies the .ipa into website/static/altstore/) and
//     publishes a GitHub release with the .ipa attached.
//
// Flags:
//   --yes            Actually perform the release (build, sign, publish,
//                     commit). Without it, every subcommand only prints
//                     its plan and changes nothing -- the safe default,
//                     since this touches production signing keys and
//                     public distribution channels.
//   --push           After committing the resulting repo changes locally,
//                     also `git push origin main`. Requires --yes. Without
//                     it the commit is local only, so a human can review
//                     `git show` before pushing to the shared branch.
//   --notes-file      Path to release notes (Markdown). If omitted, notes
//                     are drafted from the platform directory's commit
//                     subjects since that platform's last tag -- a
//                     starting point, not a substitute for real notes on
//                     anything but a trivial patch release.
//   --skip-existing   For `status`-detected gaps only: with `android`/`ios`,
//                     don't fail if this exact version is already published
//                     everywhere -- just report it and exit 0. Useful for
//                     scripted/CI invocation where "already released" isn't
//                     an error.

import {
  readFileSync,
  writeFileSync,
  existsSync,
  cpSync,
  rmSync,
  mkdirSync,
  readdirSync,
  statSync,
} from "node:fs";
import { execFileSync, execSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import path from "node:path";

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "..", "..");

const args = process.argv.slice(2);
const command = args[0];
const flag = (name) => args.includes(`--${name}`);
const option = (name) => {
  const i = args.indexOf(`--${name}`);
  return i === -1 ? undefined : args[i + 1];
};

const YES = flag("yes");
const PUSH = flag("push");
const SKIP_EXISTING = flag("skip-existing");

if (PUSH && !YES) {
  fail("--push requires --yes (nothing is committed to push without it).");
}

function fail(message) {
  console.error(`\n✗ ${message}\n`);
  process.exit(1);
}

function run(cmd, cmdArgs, opts = {}) {
  console.log(`$ ${cmd} ${cmdArgs.join(" ")}`);
  return execFileSync(cmd, cmdArgs, {
    cwd: root,
    stdio: opts.capture ? ["ignore", "pipe", "inherit"] : "inherit",
    encoding: opts.capture ? "utf8" : undefined,
    ...opts,
  });
}

function runIn(dir, cmd, cmdArgs, opts = {}) {
  console.log(`$ (cd ${dir} && ${cmd} ${cmdArgs.join(" ")})`);
  return execFileSync(cmd, cmdArgs, {
    cwd: path.join(root, dir),
    stdio: opts.capture ? ["ignore", "pipe", "inherit"] : "inherit",
    encoding: opts.capture ? "utf8" : undefined,
    ...opts,
  });
}

function quiet(cmd) {
  try {
    return execSync(cmd, { cwd: root, encoding: "utf8" }).trim();
  } catch {
    return null;
  }
}

// ── Version sources ──────────────────────────────────────────────
// Mirrors tools/legal/render.mjs's readers exactly, so "the version" can
// never mean two different things between the legal docs and a release.

function readAndroidVersion() {
  const gradlePath = path.join(root, "Android", "app", "build.gradle.kts");
  const gradle = readFileSync(gradlePath, "utf8");
  const name = gradle.match(/versionName\s*=\s*"([^"]+)"/);
  const code = gradle.match(/versionCode\s*=\s*(\d+)/);
  if (!name || !code) {
    fail(`Could not find versionName/versionCode in ${path.relative(root, gradlePath)}`);
  }
  return { name: name[1].trim(), code: Number(code[1]) };
}

function readIosVersion() {
  const pbxprojPath = path.join(root, "iOS", "Inkwell.xcodeproj", "project.pbxproj");
  const lines = readFileSync(pbxprojPath, "utf8").split("\n");
  for (let i = 0; i < lines.length; i++) {
    if (!lines[i].includes("PRODUCT_BUNDLE_IDENTIFIER = uk.ewancroft.Inkwell;")) continue;
    const window = lines.slice(Math.max(0, i - 30), i).join("\n");
    const marketing = window.match(/MARKETING_VERSION = ([^;]+);/);
    const build = window.match(/CURRENT_PROJECT_VERSION = ([^;]+);/);
    if (marketing && build) {
      return { name: marketing[1].trim(), build: Number(build[1].trim()) };
    }
  }
  fail(`Could not find the Inkwell app target's version in ${path.relative(root, pbxprojPath)}`);
}

// ── Published-state readers ──────────────────────────────────────

function ghReleaseExists(tag) {
  const out = quiet(`gh release view ${tag} --repo ewanc26/inkwell --json tagName 2>/dev/null`);
  return out !== null;
}

function gitTagExists(tag) {
  return quiet(`git rev-parse -q --verify refs/tags/${tag}`) !== null;
}

function altstoreLatest() {
  const p = path.join(root, "website", "static", "altstore", "source.json");
  const data = JSON.parse(readFileSync(p, "utf8"));
  const versions = data.apps[0].versions ?? [];
  return versions[0] ?? null; // newest entry is first, by repo convention
}

function fdroidLatest() {
  const p = path.join(root, "Android", "fdroid-repo", "metadata", "uk.ewancroft.inkwell.yml");
  const yaml = readFileSync(p, "utf8");
  const version = yaml.match(/CurrentVersion:\s*'?([^'\n]+)'?/);
  const code = yaml.match(/CurrentVersionCode:\s*(\d+)/);
  return version && code ? { name: version[1].trim(), code: Number(code[1]) } : null;
}

// ── status ────────────────────────────────────────────────────────

function status() {
  const android = readAndroidVersion();
  const ios = readIosVersion();

  console.log(`Source versions:`);
  console.log(`  iOS      ${ios.name} (build ${ios.build})`);
  console.log(`  Android  ${android.name} (versionCode ${android.code})\n`);

  const iosTag = `ios-v${ios.name}`;
  const androidTag = `android-v${android.name}`;

  const rows = [];

  const altstore = altstoreLatest();
  rows.push([
    "iOS · AltStore",
    altstore && altstore.version === ios.name && altstore.buildVersion === String(ios.build),
    altstore ? `${altstore.version} (build ${altstore.buildVersion})` : "unpublished",
  ]);
  rows.push([
    "iOS · GitHub Release",
    ghReleaseExists(iosTag),
    ghReleaseExists(iosTag) ? iosTag : `${iosTag} missing`,
  ]);

  const fdroid = fdroidLatest();
  rows.push([
    "Android · F-Droid",
    fdroid && fdroid.name === android.name && fdroid.code === android.code,
    fdroid ? `${fdroid.name} (versionCode ${fdroid.code})` : "unpublished",
  ]);
  rows.push([
    "Android · GitHub Release",
    ghReleaseExists(androidTag),
    ghReleaseExists(androidTag) ? androidTag : `${androidTag} missing`,
  ]);

  let allCurrent = true;
  for (const [label, current, detail] of rows) {
    console.log(`  ${current ? "✓" : "✗"}  ${label.padEnd(26)} ${detail}`);
    if (!current) allCurrent = false;
  }

  console.log();
  if (allCurrent) {
    console.log("All channels match the source version. Nothing to publish.");
  } else {
    console.log(
      "Gaps above. Run `node tools/release/publish.mjs android --yes` and/or\n" +
        "`node tools/release/publish.mjs ios --ipa <path> --yes` to close them.",
    );
  }
}

// ── Release notes ────────────────────────────────────────────────

function draftNotes(platformDir, sinceTag) {
  const range = sinceTag && gitTagExists(sinceTag) ? `${sinceTag}..HEAD` : "HEAD";
  const log = quiet(
    `git log --format=%s ${range} -- ${platformDir}`,
  );
  const subjects = (log ?? "").split("\n").filter(Boolean);
  if (subjects.length === 0) {
    return `No ${platformDir} changes recorded since ${sinceTag ?? "the start of history"}.`;
  }
  return subjects.map((s) => `- ${s}`).join("\n");
}

function resolveNotes(platformDir, sinceTag) {
  const notesFile = option("notes-file");
  if (notesFile) {
    const p = path.resolve(notesFile);
    if (!existsSync(p)) fail(`--notes-file ${notesFile} does not exist.`);
    return readFileSync(p, "utf8");
  }
  console.log(
    "\n(No --notes-file given -- drafting release notes from commit subjects.\n" +
      " This is a starting point, not finished copy; pass --notes-file for a real release.)\n",
  );
  return draftNotes(platformDir, sinceTag);
}

// ── android ───────────────────────────────────────────────────────

async function publishAndroid() {
  const { name, code } = readAndroidVersion();
  const tag = `android-v${name}`;

  if (ghReleaseExists(tag)) {
    const msg = `${tag} is already published on GitHub.`;
    if (SKIP_EXISTING) {
      console.log(`${msg} --skip-existing set, nothing to do.`);
      return;
    }
    fail(`${msg} Bump versionName/versionCode first, or pass --skip-existing.`);
  }

  console.log(`Publishing Android ${name} (versionCode ${code})${YES ? "" : " [dry run]"}\n`);

  const apkName = `Inkwell-${name}.apk`;
  const fdroidRepoDir = path.join(root, "Android", "fdroid-repo");
  const builtApk = path.join(root, "Android", "app", "build", "outputs", "apk", "release", "app-release.apk");
  const repoApk = path.join(fdroidRepoDir, "repo", apkName);
  const websiteFdroidRepo = path.join(root, "website", "static", "fdroid", "repo");
  const metadataPath = path.join(fdroidRepoDir, "metadata", "uk.ewancroft.inkwell.yml");

  if (!YES) {
    console.log("Would run:");
    console.log("  1. ./gradlew clean assembleRelease lint   (in Android/)");
    console.log("  2. verify the APK is signed with the release keystore (not debug)");
    console.log(`  3. copy the APK to Android/fdroid-repo/repo/${apkName}`);
    console.log(`  4. bump metadata/uk.ewancroft.inkwell.yml to ${name} / ${code}`);
    console.log("  5. fdroid update --clean   (in Android/fdroid-repo/)");
    console.log("  6. mirror the regenerated repo/ into website/static/fdroid/repo");
    console.log(`  7. gh release create ${tag} <apk> --title ... --notes ...`);
    console.log("  8. git commit the resulting changes" + (PUSH ? " and push" : " (local only, pass --push to push)"));
    console.log("\nRe-run with --yes to actually do this.");
    return;
  }

  runIn("Android", "./gradlew", ["clean", "assembleRelease", "lint"]);

  if (!existsSync(builtApk)) {
    fail(`Expected a release APK at ${path.relative(root, builtApk)} but the build didn't produce one.`);
  }

  // Confirm this is signed with the release key, not accidentally the debug
  // key -- publishing a debug-signed APK would break every existing
  // install's upgrade path with no recovery but a full uninstall/reinstall.
  const sdkDir = readFileSync(path.join(root, "Android", "local.properties"), "utf8").match(
    /sdk\.dir=(.+)/,
  )?.[1];
  if (!sdkDir) fail("Could not read Android/local.properties' sdk.dir to find apksigner.");
  const buildTools = readdirSync(path.join(sdkDir, "build-tools")).sort().at(-1);
  const apksigner = path.join(sdkDir, "build-tools", buildTools, "apksigner");
  const verify = run(apksigner, ["verify", "--print-certs", builtApk], { capture: true });
  if (!verify.includes("SHA-256 digest")) {
    fail("apksigner could not verify the built APK's signature -- refusing to publish it.");
  }
  console.log(verify.split("\n").find((l) => l.includes("SHA-256")) ?? "");

  cpSync(builtApk, repoApk);

  let metadata = readFileSync(metadataPath, "utf8");
  metadata = metadata
    .replace(/CurrentVersion:\s*'[^']*'/, `CurrentVersion: '${name}'`)
    .replace(/CurrentVersionCode:\s*\d+/, `CurrentVersionCode: ${code}`);
  writeFileSync(metadataPath, metadata);

  runIn("Android/fdroid-repo", "fdroid", ["update", "--clean"]);

  rmSync(websiteFdroidRepo, { recursive: true, force: true });
  mkdirSync(websiteFdroidRepo, { recursive: true });
  cpSync(path.join(fdroidRepoDir, "repo"), websiteFdroidRepo, {
    recursive: true,
    filter: (src) => !src.includes(`${path.sep}status`),
  });

  const notes = resolveNotes("Android/", `android-v${previousAndroidTagGuess(name)}`);
  const notesPath = path.join(root, ".release-notes-android.md");
  writeFileSync(notesPath, notes);

  run("gh", [
    "release",
    "create",
    tag,
    repoApk,
    "--repo",
    "ewanc26/inkwell",
    "--title",
    `Inkwell for Android ${name} (versionCode ${code})`,
    "--notes-file",
    notesPath,
  ]);
  rmSync(notesPath);

  commitAndMaybePush(
    ["Android/fdroid-repo", "website/static/fdroid"],
    `chore(android,fdroid): release ${name} (versionCode ${code})`,
  );

  console.log(`\n✓ Android ${name} published: F-Droid repo updated, ${tag} released on GitHub.`);
}

// Best-effort: find the highest existing android-v* tag below this version,
// for drafting notes from "everything since the last release". Not load
// bearing -- if it guesses wrong, draftNotes just walks more history than
// strictly needed, which is a longer draft, not a wrong one.
function previousAndroidTagGuess(currentName) {
  const tags = (quiet("git tag -l 'android-v*'") ?? "")
    .split("\n")
    .filter(Boolean)
    .map((t) => t.replace("android-v", ""))
    .filter((v) => v !== currentName);
  return tags.sort(compareSemver).at(-1) ?? "";
}
function previousIosTagGuess(currentName) {
  const tags = (quiet("git tag -l 'ios-v*'") ?? "")
    .split("\n")
    .filter(Boolean)
    .map((t) => t.replace("ios-v", ""))
    .filter((v) => v !== currentName);
  return tags.sort(compareSemver).at(-1) ?? "";
}
function compareSemver(a, b) {
  const pa = a.split(".").map(Number);
  const pb = b.split(".").map(Number);
  for (let i = 0; i < 3; i++) {
    if ((pa[i] ?? 0) !== (pb[i] ?? 0)) return (pa[i] ?? 0) - (pb[i] ?? 0);
  }
  return 0;
}

// ── ios ───────────────────────────────────────────────────────────

async function publishIos() {
  const { name, build } = readIosVersion();
  const tag = `ios-v${name}`;
  const ipaArg = option("ipa");

  if (ghReleaseExists(tag)) {
    const msg = `${tag} is already published on GitHub.`;
    if (SKIP_EXISTING) {
      console.log(`${msg} --skip-existing set, nothing to do.`);
      return;
    }
    fail(`${msg} Bump MARKETING_VERSION/CURRENT_PROJECT_VERSION first, or pass --skip-existing.`);
  }

  if (!ipaArg) {
    fail(
      "iOS archiving needs an interactive Xcode keychain unlock and can't run headlessly here.\n" +
        "  Export an unsigned .ipa yourself (Product > Archive > Distribute App), then re-run with\n" +
        `  --ipa /path/to/Inkwell-${name}.ipa`,
    );
  }
  const ipaPath = path.resolve(ipaArg);
  if (!existsSync(ipaPath)) fail(`--ipa ${ipaArg} does not exist.`);

  console.log(`Publishing iOS ${name} (build ${build})${YES ? "" : " [dry run]"}\n`);

  const destIpaName = `Inkwell-${name}.ipa`;
  const destIpaPath = path.join(root, "website", "static", "altstore", destIpaName);
  const sourceJsonPath = path.join(root, "website", "static", "altstore", "source.json");

  if (!YES) {
    console.log("Would run:");
    console.log(`  1. copy ${ipaArg} to website/static/altstore/${destIpaName}`);
    console.log(`  2. prepend a new versions[] entry to source.json (version ${name}, build ${build})`);
    console.log(`  3. gh release create ${tag} <ipa> --title ... --notes ...`);
    console.log("  4. git commit the resulting changes" + (PUSH ? " and push" : " (local only, pass --push to push)"));
    console.log("\nRe-run with --yes to actually do this.");
    return;
  }

  cpSync(ipaPath, destIpaPath);
  const size = statSync(destIpaPath).size;

  const data = JSON.parse(readFileSync(sourceJsonPath, "utf8"));
  const app = data.apps[0];
  const downloadURL = `https://inkwell.ewancroft.uk/altstore/${destIpaName}`;
  const notes = resolveNotes("iOS/", `ios-v${previousIosTagGuess(name)}`);

  app.version = name;
  app.buildVersion = String(build);
  app.versionDate = new Date().toISOString().slice(0, 10);
  app.size = size;
  app.downloadURL = downloadURL;
  app.versions = app.versions ?? [];
  app.versions.unshift({
    version: name,
    date: app.versionDate,
    localizedDescription: notes.slice(0, 4000),
    downloadURL,
    size,
    buildVersion: String(build),
    minOSVersion: app.minOSVersion,
  });
  writeFileSync(sourceJsonPath, JSON.stringify(data, null, 2) + "\n");

  const notesPath = path.join(root, ".release-notes-ios.md");
  writeFileSync(notesPath, notes);
  run("gh", [
    "release",
    "create",
    tag,
    destIpaPath,
    "--repo",
    "ewanc26/inkwell",
    "--title",
    `Inkwell for iOS ${name} (build ${build})`,
    "--notes-file",
    notesPath,
  ]);
  rmSync(notesPath);

  commitAndMaybePush(
    ["website/static/altstore"],
    `chore(ios): release ${name} (build ${build})`,
  );

  console.log(`\n✓ iOS ${name} published: AltStore source updated, ${tag} released on GitHub.`);
}

// ── git commit / push ────────────────────────────────────────────

function commitAndMaybePush(paths, message) {
  run("git", ["add", ...paths]);
  const staged = quiet("git diff --cached --name-only");
  if (!staged) {
    console.log("Nothing changed to commit (channel was already at this version).");
    return;
  }
  run("git", [
    "commit",
    "-m",
    `${message}\n\nPublished via tools/release/publish.mjs.\n\nCo-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`,
  ]);
  if (PUSH) {
    run("git", ["push", "origin", "main"]);
  } else {
    console.log("Committed locally. Run with --push (or `git push origin main` yourself) to publish it.");
  }
}

// ── entry point ───────────────────────────────────────────────────

const commands = { status, android: publishAndroid, ios: publishIos };
if (!commands[command]) {
  console.error(
    "Usage: node tools/release/publish.mjs <status|android|ios> [--yes] [--push] [--ipa <path>] [--notes-file <path>] [--skip-existing]",
  );
  process.exit(1);
}
await commands[command]();
