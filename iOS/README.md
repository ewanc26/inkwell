# iOS

Inkwell for iOS — the primary SwiftUI client.

## Contents

- `Inkwell/` — app source (SwiftUI views, models, rendering)
- `Inkwell.xcodeproj/` — Xcode project
- `InkwellTests/` — unit tests
- `altstore/` — AltStore source metadata
- `oauth/` — OAuth client metadata
- `logo-dark.svg` / `logo-light.svg` — app wordmark
- `LICENSE` — AGPL 3.0

## Building

Open `Inkwell.xcodeproj` in Xcode and run the `Inkwell` scheme on an iOS 26.0+ simulator or device.

## Tests

```bash
xcodebuild -project Inkwell.xcodeproj -scheme Inkwell \
  -destination 'platform=iOS Simulator,name=<available iOS 18+ device>' \
  -skip-testing:InkwellUITests build test
```

`InkwellTests/StandardSiteTests.swift` is the only unit test source — nine tests over NSID namespacing, AT-URI rejection, publication/document association and canonical URLs, verification endpoints, wire keys, search v2 decoding, notification JSON, and malformed-record tolerance. `InkwellUITests` has no source files and fails to load its bundle if run, hence the skip.

This target does not cover the shared KMP core. Those 135 tests live in `../shared/src/commonTest/` and run through Gradle from `../Android`: `./gradlew :shared:jvmTest`.

## Distribution

`altstore/source.json` must match the app's bundle version, build, privacy permissions, hosted icon, IPA byte size, and release notes.

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Licence

AGPL 3.0 — see `LICENSE`
