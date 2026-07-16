# AGENTS.md

Guidance for agents working on Inkwell for Android, the Kotlin/Jetpack Compose counterpart to the iOS Inkwell app.

## Boundaries

- `app/src/main/` contains Compose UI, navigation, view models, data/network layers, resources, and manifest configuration.
- `app/src/test/` and `app/src/androidTest/` cover JVM and device behavior.
- `docs/` records architecture and interoperability decisions. Keep behavior aligned with iOS where the README identifies shared product contracts, not by copying platform-specific implementation.

## Invariants

- Use coroutines and structured concurrency; never block the main dispatcher with PDS or file work.
- Keep UI state lifecycle-aware and avoid duplicate writes during recomposition.
- Store OAuth tokens in Android secure storage and keep secrets/log headers out of source and diagnostics.
- Preserve AT Protocol/open-union content, facets, blobs, and record revisions.
- Support accessibility, dark theme, font scaling, back navigation, and process recreation.

## Validation

Run `./gradlew build`, `./gradlew test`, and relevant lint/instrumentation tasks. Test OAuth callback/state failure, session restore, rotate/process death, document round trips, offline/error states, Unicode, images, large fonts, and TalkBack semantics. Do not commit local SDK paths, signing keys, or build output.
