# Inkwell changelog

This changelog covers every published Inkwell release and the tagged development builds that preceded the platform-specific release history. Dates are release dates where known; development snapshots use their tagged commit dates. The earliest Android releases were backfilled on GitHub after their original distribution through Inkwell's F-Droid repository.

Inkwell used generic `v*` and date-based tags during early development. Since 1.0, distributable builds use separate `ios-v*` and `android-v*` tags because the two apps have independent build numbers and artifacts.

## 2.6.0 — 2026-08-29

[iOS build 59](https://github.com/ewanc26/inkwell/releases/tag/ios-v2.6.0) · [Android versionCode 13](https://github.com/ewanc26/inkwell/releases/tag/android-v2.6.0)

### Added

- Reader live updates now add subscribed documents to the in-app notification history and issue local alerts while the app is active.

### Changed

- Android notification polling now resolves each publication from its owning PDS, so subscriptions continue to work across PDS providers.
- Live repository events are filtered to the exact publications a reader follows before they update the feed or create a notification.

### Improved

- Android reader cards, notification rows, and the unread-notifications control provide clearer TalkBack labels and button actions.

## 2.5.0 — 2026-08-28

[iOS build 58](https://github.com/ewanc26/inkwell/releases/tag/ios-v2.5.0) · [Android versionCode 12](https://github.com/ewanc26/inkwell/releases/tag/android-v2.5.0)

Inkwell 2.5.0 brings the iOS and Android apps closer together, with shared real-time feeds, publication-aware reading, profiles, reporting, moderation, offline support, and accessibility improvements.

### Added

- Cache-first feeds with shared Jetstream-powered live updates.
- Segmented Discover search for publications and AT Protocol accounts.
- Publication detail views and account profile screens.
- Native report actions on feed cards, profiles, and content on both platforms.
- Labeler preferences, keyword filters, moderation labels, and explicit reveal controls.
- Offline document caching and queued subscriptions, recommendations, and comments that replay when connectivity returns.

### Changed

- Reader cards and full publication views now use publication names, descriptions, icons, and readable publication themes instead of falling back to hostnames.
- Feed and detail loading is more responsive through parallel requests, caching, and stabilized Jetstream subscriptions.
- Reader, Discover, Writer, profile, Settings, and moderation flows have clearer accessibility announcements and better reduced-motion behaviour.

### Fixed

- Restored publication themes and moderation labels when reading cached content.
- Fixed iOS navigation resets and DPoP request failures.
- Fixed Android navigation lint compatibility and several cross-platform feed responsiveness issues.

## 2.4.0 — 2026-08-26

[iOS build 57](https://github.com/ewanc26/inkwell/releases/tag/ios-v2.4.0) · [Android versionCode 11](https://github.com/ewanc26/inkwell/releases/tag/android-v2.4.0)

### Added

- Native account and content reporting through `com.atproto.moderation.createReport` on iOS and Android.
- An optional `uk.ewancroft.inkwell.user` declaration record, with the required OAuth write scope.
- Constellation-backed discovery of people who have declared that they use Inkwell, surfaced on the website.

### Fixed

- Corrected the website's server-rendered user carousel data flow and homepage loader.
- Corrected homepage and header logo proportions.

## 2.3.0 — 2026-08-25

[iOS build 57](https://github.com/ewanc26/inkwell/releases/tag/ios-v2.3.0) · [Android versionCode 10](https://github.com/ewanc26/inkwell/releases/tag/android-v2.3.0)

### Added

- Accessibility controls for text size, bold text, increased contrast, and underlined links.
- Appearance controls for accent colour, reading font, and light, dark, or system theme.
- A one-time, dismissible support prompt when customisation is first used; appearance controls no longer require a licence key.
- Automated release tooling and CI checks for the shared KMP tests and F-Droid mirror.

### Changed

- Added an AGPL app-store distribution exception to the legal documentation.
- Updated OAuth scopes and release guidance to match the apps' real capabilities.

### Fixed

- Android now verifies feed documents whose site association is expressed as an AT URI.
- Restored iOS mute and block management parity.
- Removed the empty iOS UI test target and tracked Xcode user state.

## 2.2.0 — 2026-08-21

[iOS build 55](https://github.com/ewanc26/inkwell/releases/tag/ios-v2.2.0) · [Android versionCode 9](https://github.com/ewanc26/inkwell/releases/tag/android-v2.2.0)

### Added

- A full Settings screen with notification controls and a shortcut to system notification settings.
- An in-app notification history that can be reviewed or cleared.
- Accessibility controls for text size, bold text, increased contrast, and underlined links.
- Appearance controls for accent colour, reading font, and theme.

### Changed

- Legal and About destinations now share one settings home.
- Privacy Policy and Terms render natively on Android from the same generated source used by iOS and the website.
- Tapping a notification now opens the relevant content, and Android's unread count is kept in sync.

## 2.1.1 — 2026-08-21

[iOS build 54](https://github.com/ewanc26/inkwell/releases/tag/ios-v2.1.1) · iOS only

This documentation-only iOS release recompiles corrected Privacy Policy and Terms text into the app.

### Fixed

- Updated stale iOS and Android version references.
- Corrected notification and Android-readiness claims.
- Accurately documented how iOS Keychain items may be restored from encrypted backups.
- Disclosed the Bluesky CDN and public API, public feedback records, Vercel hosting, and support links.
- Added UK GDPR controller, legal-basis, retention, transfer, rights, and ICO information.
- Added the consumer-law carve-outs, content ownership, minimum age, termination, governing-law, and AGPL precedence terms.

## 2.1.0 — 2026-08-21

[iOS build 53 tag](https://github.com/ewanc26/inkwell/tree/ios-v2.1.0) · [Android versionCode 8](https://github.com/ewanc26/inkwell/releases/tag/android-v2.1.0)

### Added

- A real-data testing mode for release screenshots. It keeps the signed-in session and real network reads while blocking every write and showing a testing notice.
- A Bluesky supporters section in Credits.

### Fixed

- Credits can be opened while signed out or when no publications are available.
- Cleaned up the developer row and fixed broken public API calls used by Credits.

## 2.0.0 — 2026-08-18

[iOS build 52](https://github.com/ewanc26/inkwell/releases/tag/ios-v2.0.0) · [Android versionCode 7](https://github.com/ewanc26/inkwell/releases/tag/android-v2.0.0)

### Added

- Discover is now a persistent iOS tab alongside Read and Write, matching Android navigation.
- iOS reader cards show author names; Discover rows show publication descriptions, format tags, and subscription state.
- Both writers gained formatting controls, preview support, and loss reporting as part of the shared-core work leading into 2.0.

### Changed

- Lowered the iOS deployment target from iOS 26 to iOS 18, with fallbacks for the two newer visual effects.
- Split oversized iOS and Android source files into focused modules without changing their behaviour.

### Fixed

- Connected the previously inactive iOS image-picker formatting action.
- Replaced the missing SF Symbol used by the iOS editing banner.
- Reset Android's feedback dialog after a successful submission instead of leaving it on the thank-you screen for the rest of the session.

## 1.3.1 — 2026-08-16

[Android versionCode 6](https://github.com/ewanc26/inkwell/releases/tag/android-v1.3.1) · Android only

- Corrected source-code URLs after the repositories were consolidated into the Inkwell monorepo.
- Rebuilt the signed APK and regenerated the self-hosted F-Droid repository metadata.
- Cleaned duplicate historical F-Droid index entries.

The GitHub release and platform tag were created retroactively. The attached APK is the original artifact distributed through Inkwell's F-Droid repository; the tag points to the earliest available Android commit after its earlier history was squash-imported.

## 1.3.0 — 2026-08-16

[Android versionCode 5](https://github.com/ewanc26/inkwell/releases/tag/android-v1.3.0) · Android only

- Aligned Android post and feed-card styling with iOS, including colours, chevrons, outlines, and the splash screen.
- Published the signed APK through the self-hosted F-Droid repository.

This short-lived release was superseded by 1.3.1 later the same day. Its platform tag and GitHub Release were created retroactively on 2026-08-28 using the exact original APK preserved in the repository's F-Droid history.

## 1.2.0 — 2026-08-16

[Android versionCode 4](https://github.com/ewanc26/inkwell/releases/tag/android-v1.2.0) · Android only

- Fixed Android compilation errors in reader and writer screens.
- Improved ViewModels, navigation, Discover, and the data layer.
- Added release screenshots and refreshed reader/feed-card styling to better match iOS.
- Published the signed APK through the self-hosted F-Droid repository.

The GitHub release and platform tag were created retroactively after the Android history was squash-imported.

## 1.1.0 — 2026-08-16

[Android versionCode 3](https://github.com/ewanc26/inkwell/releases/tag/android-v1.1.0) · Android only

- Added facets, comments, previous/next navigation, duplicate removal, and more Leaflet block types to the reader.
- Added format-aware publishing for Leaflet, Markpub, pckt, and Offprint content.
- Added the associated comment and content wire models.
- Published the release through the self-hosted F-Droid repository.

The GitHub release and platform tag were created retroactively after the Android history was squash-imported.

## 1.0.1 — 2026-08-15

[Android versionCode 2](https://github.com/ewanc26/inkwell/releases/tag/android-v1.0.1) · Android only

- Fixed publication search decoding against the real v2 API response.
- Tolerated a `null` search-result total instead of failing the response.
- Updated the F-Droid artifact and legal version references.

The GitHub release and platform tag were created retroactively after the Android history was squash-imported.

## 1.0.0 — 2026-08-15–16

[iOS build 50](https://github.com/ewanc26/inkwell/releases/tag/ios-v1.0.0) · [Android versionCode 1](https://github.com/ewanc26/inkwell/releases/tag/android-v1.0.0) · [historical generic tag](https://github.com/ewanc26/inkwell/tree/v1.0.0)

### iOS

- First stable iOS release of Inkwell's native Standard.site reader, Discover, and writer experience.
- Fixed OAuth token exchange on PDS providers that do not expose DPoP nonces on `GET`: the token endpoint is pre-flown with a sanitized `POST` before the real authorization code is presented.
- Polished login, Discover, reader, theme, and navigation UI for the release.

The iOS GitHub release was backfilled when platform-specific tags were introduced. Its attached IPA came from the live AltStore URL shared by the build 49 and 50 entries, so the artifact cannot be independently verified as build 50.

### Android

- First distributed Android release of the native Standard.site reader and writer.
- Included OAuth sign-in, publication discovery, reading and publishing, and the initial self-hosted F-Droid distribution.

The Android GitHub release and platform tag were also created retroactively after the app's earlier history was squash-imported.

## Tagged development history

These tags predate the formal iOS/Android GitHub release process. Version numbers were briefly reused between the separate pre-monorepo projects, so they are recorded here as tagged snapshots rather than presented as one continuous cross-platform semantic-version sequence.

### 0.16.0 — 2026-06-22

- Added blob-backed Leaflet pages and comment UI in the iOS reader.
- Also tagged `v2026-06-22`.

### 0.15.1 — 2026-06-22

- Fixed Sendable conformance and a Swift type-checker timeout that prevented the prior build from compiling cleanly.

### 0.15.0 — 2026-06-22

- Threaded lossy facet tracking through the facet converter so format conversions can report what cannot round-trip.

### 0.14.0 — 2026-06-22

- Added `pub.leaflet.comment` models, lexicon registration, and create/read/update/delete support.

### 0.13.0 — 2026-06-22

- Added image-blob round-tripping through the writer context.

### 0.12.0 — 2026-06-22

- Added reader and writer parity with standard.horse for Leaflet, pckt, and Offprint content providers.

### 0.11.8 — 2026-06-22

- Replaced regex literals with alternatives compatible with the supported Swift toolchain.

### 0.11.7 — 2026-06-22

- Matched standard.horse ordered-list parsing and lossy facet tracking.

### 0.11.6 — 2026-06-22

- Fixed Swift actor-isolation errors in the following-feed task group.

### 0.11.5 — 2026-06-22

- Loaded the following feed in parallel with an eight-second timeout per remote PDS.

### 0.11.4 — 2026-06-22

- Collapsed the cover-image area when an image is absent or fails to load.

### 0.11.3 — 2026-06-22

- Used unauthenticated `listRecords` requests for public records to avoid DPoP nonce errors.

### 0.11.2 — 2026-06-22

- Added `createdAt` to publication records and clearer writer error details.

### 0.11.1 — 2026-06-22

- Restored the `atproto` OAuth scope after the granular-scope experiment prevented authentication.

### 0.11.0 — 2026-06-22

- Replaced the blanket OAuth scope with granular repository and blob scopes. This was adjusted in 0.11.1 for provider compatibility.

### 0.10.6 — 2026-06-22

- Combined the Swift actor-isolation and RFC 8252 redirect-URI fixes into a buildable release.

### 0.10.5 — 2026-06-22

- Switched OAuth to an RFC 8252 reverse-DNS redirect URI scheme.

### 0.10.4 — 2026-06-22

- Fixed Swift actor isolation while constructing login storage.

### 0.10.3 — 2026-06-22

- Replaced the placeholder Privacy Policy effective date with a concrete date.

### 0.10.2 — 2026-06-22

- Corrected the OAuth client-metadata URL to `inkwell.ewancroft.uk`.

### 0.10.1 — 2026-06-21

- Guarded against layout overflow and improved Dynamic Type support.
- Also tagged `v2026-06-21`.

### 0.10.0 — 2026-06-21

- Added reader subscription and recommendation actions, Discover thumbnails, and overflow guards.

### 0.9.0 — 2026-06-20

- Added the Privacy Policy and Terms of Service.
- Also tagged `v2026-06-20`.

### 0.8.4 — 2026-06-20

- Corrected the wordmark colouring for accurate Display P3 rendering.

### 0.8.3 — 2026-06-20

- Used the exact Display P3 ink-drop colour in the shared Inkwell mark.

### 0.8.2 — 2026-06-20

- Restored the missing login icon and removed legacy icon assets.

### 0.8.1 — 2026-06-20

- Increased the legibility of the tinted app icon.

### 0.8.0 — 2026-06-20

- Added subscription notifications and refresh support.

### 0.7.0 — 2026-06-20

- Added the first Standard.site reader and writer flows.

### 0.6.0 — 2026-06-20

- Added initial Standard.site content support.

### 0.5.1 — 2026-06-19

- Fixed asynchronous configuration loading.
- Also tagged `v2026-06-19`.

### 0.5.0 — 2026-06-19

- Added account persistence.

### 0.4.0 — 2026-06-19

- Added the initial tab-based app structure.

### 0.3.0 — 2026-06-19

- Replaced the template content with Inkwell's first signed-in home view.

### 0.2.0 — 2026-06-19

- Added the branded launch screen and corrected dark-mode and Interface Builder issues.

### 0.1.1 — 2026-06-19

- Switched identity resolution to Slingshot and fixed an authentication timeout race.

### 0.1.0 — 2026-06-25

- Early Android preview tag: corrected the configured site URL from `inkwell.app` to `inkwell.ewancroft.uk`.

### Date-tagged snapshots

- `v2026-06-29`: added Android publication creation, improved cross-platform branding, and fixed PDS endpoint resolution and subscription-cache invalidation.
- `v2026-06-27`: migrated the early Android app from app-password authentication to OAuth 2.1 with DPoP, resolved Bluesky profiles in reader cards, and added the AltStore source listing.
- `v2026-06-25`: made the early Android project buildable in Android Studio and expanded source documentation.
- `v2026-06-24`: added iOS reader pagination, embeds, Constellation discovery, theme consistency, App Intents, retries, and accessibility; introduced the native Android client.
- `v2026-06-23`: expanded iOS Leaflet rendering, allowed browsing without authentication, supplemented the following feed from search, and fixed navigation and touch handling.
- `v2026-06-22`: alias of `v0.16.0`.
- `v2026-06-21`: alias of `v0.10.1`.
- `v2026-06-20`: alias of `v0.9.0`.
- `v2026-06-19`: alias of `v0.5.1`.
