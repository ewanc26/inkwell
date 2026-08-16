# Inkwell

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="iOS/logo-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="iOS/logo-light.svg">
  <img alt="Inkwell" src="iOS/logo-light.svg" width="110">
</picture>

A native reader and writer for the [Standard.site](https://standard.site) publishing ecosystem on AT Protocol.

Inkwell is a **native app** available on iOS and Android. This monorepo also contains the marketing/legal site that hosts the app's install sources and OAuth metadata.

## Repository structure

| Directory | Purpose |
|-----------|---------|
| `iOS/` | iOS app (SwiftUI) |
| `Android/` | Android app (Kotlin/Compose) |
| `website/` | Marketing/legal site and OAuth metadata (`inkwell.ewancroft.uk`) |

## Features

- Reads `site.standard.publication` and `site.standard.document` records from the author's PDS.
- Renders Markpub Markdown plus Leaflet, pckt, and Offprint content. Uses `textContent` as a fallback. Native block rendering for Leaflet (including blob-stored pages), Markdown for everything else.
- Theme resolution: Leaflet's light/dark palette → `basicTheme` → system defaults. Publication-level by default, overridable per document.
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

Inkwell-owned lexicons use the `uk.ewancroft.inkwell.*` namespace. Shared records use their canonical `site.standard.*` NSIDs.

## Design

The app icon and in-app wordmark share one set of vector coordinates, so they always match. Both are duotone: the letterform follows the system foreground colour (light/dark and tinted icon modes), while the ink drop uses one fixed brand colour in Display P3.

## Testing

### iOS

`iOS/InkwellTests/StandardSiteTests.swift` covers AT-URI parsing, record encoding/decoding, publication/document association rules, theme and verification-endpoint resolution, and tolerant decoding of malformed records.

### Android

Run `./gradlew test` from the `Android/` directory.

## Dependencies

- **iOS:** **ATProtoKit** — via Swift Package Manager (`https://github.com/MasterJ93/ATProtoKit.git`)
- **Android:** **atproto-kotlin** — via Gradle version catalog

## Support

If you find this project useful, consider supporting its development:

[![Ko-fi](https://img.shields.io/badge/Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/ewancroft)
[![GitHub Sponsors](https://img.shields.io/badge/GitHub%20Sponsors-30363D?style=for-the-badge&logo=github&logoColor=white)](https://github.com/sponsors/ewanc26)

## Licence

AGPL 3.0 — see `LICENSE`
