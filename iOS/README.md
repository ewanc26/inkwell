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

Inkwell has an iOS 18.0 deployment target, but **building the project requires Xcode 26 or newer** because the app icon uses Xcode 26's Icon Composer `.icon` format.

Open `Inkwell.xcodeproj` in Xcode, select the `Inkwell` scheme, choose an installed iOS 18+ simulator or compatible device, and run the app.

For full human contributor setup — including simulator installation, signing, Git LFS, testing mode, shared Kotlin changes, and troubleshooting — see [`../CONTRIBUTING.md`](../CONTRIBUTING.md#ios-development-in-xcode).

## Tests

From Xcode, use **Product → Test** (`⌘U`). For a command-line equivalent, substitute a simulator that is installed on your Mac:

```bash
xcodebuild -project Inkwell.xcodeproj -scheme Inkwell \
  -destination 'platform=iOS Simulator,name=<available iOS 18+ device>' \
  build test
```

The Xcode test target does not exercise the shared KMP core. Those tests live in `../shared/src/commonTest/` and run through Gradle from `../Android`:

```bash
./gradlew :shared:jvmTest
```

## Distribution

`altstore/source.json` must match the app's bundle version, build, privacy permissions, hosted icon, IPA byte size, and release notes.

The Apple App Store is a planned additional distribution channel. The AGPL-3.0 [App Store Distribution Exception](../APP_STORE_EXCEPTION.md) permits otherwise compliant App Store distribution while keeping the source and the existing AltStore Classic route available under the AGPL.

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Licence

AGPL-3.0 with the [App Store Distribution Exception](../APP_STORE_EXCEPTION.md) — see `../LICENSE` for the base licence.
