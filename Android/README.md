# Inkwell for Android

> **Experimental** — Android companion to [Inkwell for iOS](https://github.com/ewanc26/inkwell). The iOS version is the primary client. This is a work-in-progress toward feature parity.

A native reader and writer for the [Standard.site](https://standard.site) publishing ecosystem on AT Protocol. Built with Jetpack Compose, Material 3, and Kotlin.

## Features

- **Read** — Fetches `site.standard.publication` and `site.standard.document` records from the author's PDS. Three-tab layout (Read / Discover / Write). Tapping a post renders its real content: Leaflet blocks, Markpub/plain text, and a generic fallback for other formats so nothing is silently dropped. Comments and interactions aren't shown yet.
- **Discover** — Searches the Standard.site public index. Subscribe to publications and recommend documents, with live recommend counts from Constellation. Paginated feed with prev/next navigation.
- **Write** — Publishes `site.standard.document` records with portable metadata. Content format selection is presentation-only for now; every publish currently writes plain text content regardless of the selected format.
- **AT Protocol Native** — OAuth with your AT Protocol handle (no app password). Session restores silently on relaunch.
- **Verification** — Publication `.well-known` and document `<link>` checks, shown as a badge on the post detail screen.

## Relationship to Inkwell iOS

| | iOS (primary) | Android (experimental) |
|---|---|---|
| Language | Swift | Kotlin |
| UI | SwiftUI | Jetpack Compose + Material 3 |
| Navigation | NavigationStack | Navigation Compose |
| Networking | URLSession + OAuthenticator | OkHttp |
| Serialization | Codable | kotlinx.serialization |
| DI | @Environment | Hilt |
| Background | BGAppRefreshTask | WorkManager |
| Status | Production-ready | Experimental |

Both share the same AT Protocol data model shapes, Constellation cross-repo discovery pattern, theme resolution cascade, and three-tab structure.

## Architecture

```
app/src/main/java/uk/ewancroft/inkwell/
├── data/
│   ├── model/Models.kt
│   ├── remote/AtProtoApi.kt
│   └── remote/ConstellationClient.kt
├── ui/
│   ├── theme/Theme.kt
│   ├── navigation/NavGraph.kt
│   ├── auth/LoginScreen.kt
│   ├── reader/ReaderScreen.kt
│   ├── writer/WriterScreen.kt
│   └── discover/DiscoverScreen.kt
├── InkwellApp.kt
└── MainActivity.kt
```

## Install

Add `https://inkwell.ewancroft.uk/fdroid/repo` as a custom repo in [F-Droid](https://f-droid.org/), [Droid-ify](https://github.com/Iamlooker/Droid-ify), or [Obtainium](https://github.com/ImranR98/Obtainium). This is a self-hosted, signed F-Droid-compatible repo maintained directly from this project — not (yet) F-Droid's official default repo, which is a separate, longer-running submission tracked in `fdroid/README.md`.

## Getting started

```bash
git clone https://github.com/ewanc26/inkwell-android.git
```

Open in Android Studio (Hedgehog or later). Build with Gradle:

```bash
./gradlew assembleDebug
```

Run on API 26+ device or emulator. Sign in with your AT Protocol handle via OAuth.

## Dependencies

- Jetpack Compose (Material 3)
- Navigation Compose
- OkHttp + kotlinx.serialization
- Hilt
- Coil

## Support

Inkwell is free on both iOS and Android. If you find it useful, a one-off tip (£2.99 suggested) is welcome but never required:

[![Ko-fi](https://img.shields.io/badge/Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/ewancroft?amount=2.99)
[![GitHub Sponsors](https://img.shields.io/badge/GitHub%20Sponsors-30363D?style=for-the-badge&logo=github&logoColor=white)](https://github.com/sponsors/ewanc26)

## Licence

AGPL 3.0 — see [Inkwell iOS](https://github.com/ewanc26/inkwell)
