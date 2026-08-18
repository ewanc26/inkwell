# Android

Inkwell for Android — the experimental Kotlin/Compose client.

## Contents

- `app/` — Android application source (Kotlin/Compose, Hilt, ViewModels)
- `build.gradle.kts` / `settings.gradle.kts` — Gradle build config
- `gradle/` — Gradle wrapper and version catalog
- `fdroid/` — F-Droid build recipe (for the official fdroiddata submission)
- `fdroid-repo/` — Self-hosted F-Droid repo workspace (signed APKs, index, metadata)
- `docs/` — OAuth client metadata and other docs
- `fastlane/` — Fastlane metadata, including phone screenshots for store listings

## Building

```bash
./gradlew assembleDebug
```

Run on API 26+ device or emulator.

## Tests

```bash
./gradlew test
```

Runs the app's two unit test sources only — `StandardSiteVerifierTest` (13 tests) and `SearchModelsTest` (2). Three verifier tests hit the real `blog.ewancroft.uk` standard.site publication over the network and fail offline. There are no instrumentation tests.

This Gradle root also owns the shared KMP module (`:shared`, mapped to `../shared`), which holds the bulk of the automated coverage — 135 tests in `shared/src/commonTest/`. The aggregate `test` task does **not** reach them, because the KMP `jvm()` target exposes `jvmTest` rather than `test`:

```bash
./gradlew :shared:jvmTest      # JVM target
./gradlew :shared:allTests     # adds Kotlin/Native iOS targets; much slower
```

## Distribution

Self-hosted signed F-Droid repo is maintained in `fdroid-repo/`. The official F-Droid submission recipe is in `fdroid/`.

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Licence

AGPL 3.0 — see `../LICENSE`
