# Contributing to Inkwell

Inkwell is a native reader and writer for the [Standard.site](https://standard.site) publishing ecosystem on [AT Protocol](https://atproto.com). This monorepo contains all three clients:

- **iOS** — primary SwiftUI app (`iOS/`)
- **Android** — experimental Jetpack Compose app (`Android/`)
- **Website** — SvelteKit marketing/legal site and OAuth metadata (`website/`)

## Before you start

Read the platform-specific `AGENTS.md` files before touching code. They are authoritative on source boundaries, invariants, and build rules:

- [`AGENTS.md`](AGENTS.md) — monorepo-wide rules and cross-references
- [`iOS/AGENTS.md`](iOS/AGENTS.md) — Keychain/DPoP rules, Xcode workflow
- [`Android/AGENTS.md`](Android/AGENTS.md) — EncryptedSharedPreferences, Gradle workflow
- [`website/AGENTS.md`](website/AGENTS.md) — legal/oauth accuracy, Vercel deployment

## Repository structure

| Directory | Purpose |
|-----------|---------|
| `iOS/` | iOS app (SwiftUI, Xcode project, AltStore distribution) |
| `Android/` | Android app (Kotlin/Compose, Gradle, F-Droid distribution) |
| `website/` | SvelteKit marketing/legal site + OAuth metadata (`inkwell.ewancroft.uk`) |

## Prerequisites

### iOS
- Xcode 15+ on macOS
- iOS 26.0+ simulator or device
- Swift 5 mode

### Android
- JDK 17
- Android SDK with API 36 (compile/target) and API 26 (minimum)
- Gradle 8.13 (wrapper included)

### Website
- Node.js 22+
- pnpm (authoritative package manager; `pnpm-lock.yaml` and `pnpm-workspace.yaml` are canonical)

## Building and testing

### Shared core

Most of the repo's automated coverage is here — 135 tests in `shared/src/commonTest/`, spanning AT-URI parsing, markdown parsing and inline scanning, facet schema and conversion, content-format conversion, URL utilities, reader themes, and the tip-prompt/notification policies. Run them from `Android/`, the only Gradle root:

```bash
cd Android
./gradlew :shared:jvmTest      # JVM target
./gradlew :shared:allTests     # adds Kotlin/Native iOS targets; much slower
```

`./gradlew test` does **not** run them — the KMP `jvm()` target exposes `jvmTest`, not `test`, so the aggregate task skips `:shared`. If you change anything under `shared/`, run `:shared:jvmTest` explicitly.

### iOS

```bash
cd iOS
xcodebuild -project Inkwell.xcodeproj -scheme Inkwell \
  -destination 'platform=iOS Simulator,name=<available iOS 18+ device>' \
  -skip-testing:InkwellUITests build test
```

`InkwellTests/StandardSiteTests.swift` is the only unit test source — nine tests covering the Inkwell NSID namespace, AT-URI rejection of malformed values, association/canonical URLs, verification endpoint paths, wire keys, search v2 decoding, notification JSON, and tolerant record pages. `InkwellUITests` has no source files and fails to load its bundle if run, hence the skip. This target does not exercise the shared core.

### Android

```bash
cd Android
./gradlew clean assembleDebug lint test
```

For release work:

```bash
./gradlew assembleRelease
```

Inspect R8 output and ensure the signed APK is produced.

App-level coverage is thin: `StandardSiteVerifierTest` (13 tests) and `SearchModelsTest` (2) are the only unit test sources, and there are no instrumentation tests. Three verifier tests hit the real `blog.ewancroft.uk` standard.site publication over the network and fail offline. Don't treat a green `./gradlew test` as behavioural coverage of the app beyond those two files — and note it skips `:shared` entirely (see above).

### Website

```bash
cd website
pnpm install --frozen-lockfile
pnpm check
pnpm build
```

Use `pnpm exec prettier --check --ignore-unknown .` for non-mutating formatting checks. There is no `lint` or test script.

## Code style and conventions

- **iOS:** Follow Swift 5 conventions. `LoginStateManager`, notification/background managers, and UI state are `@MainActor`. Do not introduce blocking I/O or uncancelled view tasks.
- **Android:** Follow existing Kotlin/Compose patterns. ViewModels own `StateFlow`; Compose screens and `NavGraph` own UI/navigation. Use structured concurrency and `Dispatchers.IO` for synchronous OkHttp. URL-encode every XRPC query value.
- **Website:** Preserve server-rendered/static markup and CSS. The OAuth endpoint (`/client-metadata.json`) must remain dynamic. Do not globally prerender without verifying deployment semantics.

## Commit conventions

Use **atomic, scoped commits** — one logical change per commit, with a clear subject line. Good examples:

- `Android: fix compilation errors in reader and writer screens`
- `iOS: add AltStore screenshot placeholders`
- `Website: add screenshot placeholders and update landing page`

Avoid mixing build fixes, feature changes, and metadata updates in a single commit.

## Security and secrets

Never commit secrets, keys, or credentials. This includes:

- `local.properties` (Android SDK path)
- `keystore.properties` and `*.keystore` / `*.jks` (Android signing)
- `Android/fdroid-repo/config.yml` and `keystore.p12` (F-Droid repo signing)
- OAuth sessions, tokens, DPoP keys, auth codes
- `.idea/`, `.gradle/`, `app/build/`, `DerivedData/`, `node_modules/`

If you discover a committed secret, treat it as compromised and rotate it immediately.

## Distribution and releases

### iOS

- `iOS/altstore/source.json` must match the app's bundle version, build, privacy permissions, hosted icon/IPA byte size, and release notes.
- Never commit IPA archives or signing profiles.

### Android

- The self-hosted F-Droid repo lives in `Android/fdroid-repo/`.
- To publish a new version, build a signed release APK, copy it into `Android/fdroid-repo/repo/`, update `metadata/uk.ewancroft.inkwell.yml`, then run `fdroid update --clean` from that directory.
- Copy the regenerated `repo/` into the website's `static/fdroid/` for deployment.

### Website

- Install-source URLs are defined in `website/src/lib/config.ts`. Keep them in sync with `static/altstore/source.json` and `static/fdroid/repo/`.
- The site hosts the AltStore source, F-Droid repo, legal/privacy pages, and `/client-metadata.json`.
- Do not add analytics, forms, pixels, remote scripts, logs, cookies, or server-side user data without explicit review and policy updates.

## Issues and feature requests

- Search existing issues before opening a new one.
- For bugs, include reproduction steps, affected platform(s), and version numbers.
- For features, explain the use case and how it aligns with Inkwell's scope (Standard.site publishing on AT Protocol).
- When proposing changes that touch multiple platforms, split them into per-platform issues so they can be tracked and reviewed independently.

## Platform parity

iOS is the primary implementation. Android is experimental and materially incomplete. When working on Android:

- Do not claim parity with iOS unless the feature is fully implemented and manually tested.
- Verify behavior against live PDS/Constellation instances rather than mocks.
- Surface capability gaps explicitly in the UI rather than pretending they are complete.

## Accessibility and inclusivity

- Preserve native accessibility, Dynamic Type, dark/light behavior, reduced motion, safe-area behavior, and platform-appropriate mark/wordmark coordinates.
- Target WCAG 2.1 AA for the website.
- Test with TalkBack (Android) and VoiceOver (iOS) for new or changed screens.

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Questions?

Open an issue or reach out via [Bluesky](https://bsky.app/profile/ewancroft.uk).

If you find this project useful, you can also support its development:
- [Ko-fi](https://ko-fi.com/ewancroft)
- [GitHub Sponsors](https://github.com/sponsors/ewanc26)
