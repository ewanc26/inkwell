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

## Distribution

Self-hosted signed F-Droid repo is maintained in `fdroid-repo/`. The official F-Droid submission recipe is in `fdroid/`.

## Licence

AGPL 3.0 — see `../LICENSE`
