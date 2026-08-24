# iOS

Inkwell for iOS — the primary SwiftUI client.

## Contents

- `Inkwell/` — app source (SwiftUI views, models, rendering)
- `Inkwell.xcodeproj/` — Xcode project
- `InkwellTests/` — unit tests
- `altstore/` — AltStore source metadata
- `oauth/` — OAuth client metadata
- `logo-dark.svg` / `logo-light.svg` — app wordmark

## Building

Open `Inkwell.xcodeproj` in Xcode and run the `Inkwell` scheme on an iOS 26.0+ simulator or device.

## Tests

```bash
xcodebuild -project Inkwell.xcodeproj -scheme Inkwell \
  -destination 'platform=iOS Simulator,name=<available iOS 18+ device>' \
  -skip-testing:InkwellUITests build test
```

There are two unit test sources, twelve tests in total: `InkwellTests/StandardSiteTests.swift` (nine — NSID namespacing, AT-URI rejection, publication/document association and canonical URLs, verification endpoints, wire keys, search v2 decoding, notification JSON, malformed-record tolerance) and `InkwellTests/BSkyListModelsTests.swift` (three — `app.bsky.graph.getList` decoding and the supporters-list AT-URI). `InkwellUITests` has no source files and fails to load its bundle if run, hence the skip.

This target does not cover the shared KMP core. Those 135 tests live in `../shared/src/commonTest/` and run through Gradle from `../Android`: `./gradlew :shared:jvmTest`.

## Distribution

`altstore/source.json` must match the app's bundle version, build, privacy permissions, hosted icon, IPA byte size, and release notes.

The Apple App Store is a planned additional distribution channel. The AGPL-3.0 [App Store Distribution Exception](../APP_STORE_EXCEPTION.md) permits otherwise compliant App Store distribution while keeping the source and the existing AltStore Classic route available under the AGPL.

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Licence

AGPL-3.0 with the [App Store Distribution Exception](../APP_STORE_EXCEPTION.md) — see `../LICENSE` for the base licence.
