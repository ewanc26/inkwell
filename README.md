<h1 align="center">Inkwell</h1>

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="iOS/logo-dark.svg">
    <source media="(prefers-color-scheme: light)" srcset="iOS/logo-light.svg">
    <img alt="Inkwell" src="iOS/logo-light.svg" width="110">
  </picture>
</p>

<p align="center">
  <a href="https://github.com/ewanc26/inkwell/releases/latest?q=ios-v"><img src="https://img.shields.io/github/v/release/ewanc26/inkwell?filter=ios-v*&label=iOS&logo=apple&logoColor=white" alt="Latest iOS release"></a>
  <a href="https://github.com/ewanc26/inkwell/releases/latest?q=android-v"><img src="https://img.shields.io/github/v/release/ewanc26/inkwell?filter=android-v*&label=Android&logo=android&logoColor=white" alt="Latest Android release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/ewanc26/inkwell" alt="AGPL-3.0"></a>
  <a href="https://github.com/sponsors/ewanc26"><img src="https://img.shields.io/github/sponsors/ewanc26?logo=githubsponsors&logoColor=white&label=sponsors" alt="Sponsor"></a>
</p>

A native reader and writer for the [Standard.site](https://standard.site) publishing ecosystem on AT Protocol.

Inkwell is a **native app** available on iOS and Android. This monorepo also contains the marketing/legal site that hosts the app's install sources and OAuth metadata.

## Repository structure

| Directory | Purpose |
|-----------|---------|
| `shared/` | Kotlin Multiplatform shared core — format conversion, markdown parsing, facet handling, content models, verification URLs, and constellation deduplication. Consumed by iOS via `InkwellShared.xcframework`; consumed by Android directly. |
| `iOS/` | iOS app (SwiftUI) |
| `Android/` | Android app (Kotlin/Compose) |
| `website/` | Marketing/legal site and OAuth metadata (`inkwell.ewancroft.uk`) |

## Features

- Reads `site.standard.publication` and `site.standard.document` records from the author's PDS.
- Renders Markpub Markdown plus Leaflet, pckt, and Offprint content. Uses `textContent` as a fallback. Native block rendering for Leaflet (including blob-stored pages), Markdown for everything else.
- Real device screenshots are checked in under `iOS/screenshots/`, `Android/fastlane/metadata/.../images/phoneScreenshots/`, and `website/static/screenshots/`, captured in testing mode — `-testing` (iOS) / `--ez testing true` (Android). Testing mode uses the real signed-in session and real network reads; it intercepts every write and shows a "Testing mode" notice instead. Captures therefore require being logged in.
- Theme resolution: Leaflet's light/dark palette → `basicTheme` → system defaults. Publication-level by default, overridable per document.
- Split-pane editor with live markdown preview, formatting toolbar, and selectable content formats.
- Image upload directly into the editor.
- Loss reporting when converting between formats that don't round-trip perfectly.
- Publishes Standard.site documents with portable metadata and selectable content formats.
- Creates and removes `site.standard.graph.subscription` records and recommends.
- Searches the cross-platform Standard.site public index, fetches records directly from the author.
- Publication `.well-known` and document `<link>` verification.
- Polls subscribed publications for notifications (in-app + local), including background app refresh.
- OAuth sign-in with your AT Protocol handle (no app password). Session resumes silently on relaunch.

## Getting started

### iOS

```bash
git clone https://github.com/ewanc26/inkwell.git
cd inkwell
```

Open `iOS/Inkwell.xcodeproj` in Xcode, build and run. Sign in with your AT Protocol handle via OAuth.

### Android

```bash
git clone https://github.com/ewanc26/inkwell.git
cd inkwell/Android
```

Build with Gradle:

```bash
./gradlew assembleDebug
```

Run on API 26+ device or emulator. Sign in with your AT Protocol handle via OAuth.

## Interoperability

Standard.site standardises publishing metadata rather than one body format. Inkwell always publishes `textContent` and defaults to `at.markpub.markdown`, while retaining readers for `pub.leaflet.content`, `blog.pckt.content`, and `app.offprint.content`.

Shared logic — format conversion, markdown parsing, facet handling, content models, and verification URL construction — lives in the `shared/` Kotlin Multiplatform module. Both apps consume it through platform-appropriate wrappers, ensuring a single source of truth for wire-format rules.

Inkwell-owned lexicons use the `uk.ewancroft.inkwell.*` namespace. Shared records use their canonical `site.standard.*` NSIDs.

## Design

The app icon and in-app wordmark share one set of vector coordinates, so they always match. Both are duotone: the letterform follows the system foreground colour (light/dark and tinted icon modes), while the ink drop uses one fixed brand colour in Display P3.

## Testing

### Shared core

Most of the automated coverage lives in the KMP module: `shared/src/commonTest/` holds 135 tests across ten files, covering AT-URI parsing, markdown parsing and inline scanning, facet schema and conversion, content-format conversion, URL utilities, reader themes, and the tip-prompt/notification policies.

Run them from the `Android/` directory (the only Gradle root in the repo):

```bash
./gradlew :shared:jvmTest      # JVM target — fast, what CI-style checks should use
./gradlew :shared:allTests     # adds the Kotlin/Native iOS targets; much slower
```

Note that `./gradlew test` does **not** include these. The KMP `jvm()` target exposes `jvmTest`, not `test`, so the aggregate `test` task skips the shared module entirely.

### iOS

`iOS/InkwellTests/StandardSiteTests.swift` holds nine tests covering the Inkwell NSID namespace, AT-URI rejection of malformed values, publication/document association and canonical URLs, verification endpoint paths, standard.site wire keys, search v2 decoding, notification JSON round-tripping, and tolerant decoding of malformed records. `iOS/InkwellTests/BSkyListModelsTests.swift` adds three more over `app.bsky.graph.getList` decoding and the supporters-list AT-URI, mirroring Android's `BlueskyListModelsTest`.

```bash
xcodebuild -project iOS/Inkwell.xcodeproj -scheme Inkwell \
  -destination 'platform=iOS Simulator,name=<available iOS 18+ device>' \
  -skip-testing:InkwellUITests build test
```

`InkwellUITests` has no source files and fails to load its bundle if run, hence the skip. The shared-core tests are not part of this target — run them through Gradle as above.

### Android

```bash
./gradlew test
```

This runs the app's two unit test sources only: `StandardSiteVerifierTest` (13 tests) and `SearchModelsTest` (2). Three of the verifier tests hit the real `blog.ewancroft.uk` standard.site publication over the network and fail offline. There are no instrumentation tests.

## Dependencies

- **shared/**: **Kotlin Multiplatform** — kotlinx.serialization, kotlinx.coroutines. Compiled to an XCFramework for iOS and consumed as a Gradle module by Android.
- **iOS:** **ATProtoKit** — via Swift Package Manager (`https://github.com/MasterJ93/ATProtoKit.git`)
- **Android:** **atproto-kotlin** — via Gradle version catalog

## Support

If you find this project useful, consider supporting its development:

[![Ko-fi](https://img.shields.io/badge/Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/ewancroft)
[![GitHub Sponsors](https://img.shields.io/badge/GitHub%20Sponsors-30363D?style=for-the-badge&logo=github&logoColor=white)](https://github.com/sponsors/ewanc26)

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Licence

Inkwell is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0) with an [App Store Distribution Exception](APP_STORE_EXCEPTION.md) granted as additional permission under AGPL section 7. See `LICENSE` for the unchanged AGPL-3.0 text and `APP_STORE_EXCEPTION.md` for the additional permission.

The exception allows otherwise AGPL-compliant builds to be distributed through the Apple App Store, Google Play, and comparable stores whose terms would otherwise conflict with the AGPL. It does not remove the AGPL's source-availability or copyleft requirements.

Inkwell is not currently distributed through the App Store or Google Play. The existing free AltStore Classic and F-Droid distribution channels remain available, including if paid mainstream-store builds are introduced later.
