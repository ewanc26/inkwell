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

Run `xcodebuild -project Inkwell.xcodeproj -scheme Inkwell -destination 'platform=iOS Simulator,name=<available iOS 26.5 device>' build test`.

## Distribution

`altstore/source.json` must match the app's bundle version, build, privacy permissions, hosted icon, IPA byte size, and release notes.

## Licence

AGPL 3.0 — see `LICENSE`
