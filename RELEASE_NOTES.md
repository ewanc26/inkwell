# Inkwell changelog

This is the release history for Inkwell. The current releases are unified across iOS and Android: one version, one GitHub release, and both platform artefacts where that version shipped on both platforms.

The older history is a bit messier because Inkwell started as separate iOS and Android projects. Some of those releases were backfilled after the monorepo was created, and the old `ios-v*` / `android-v*` tags are kept around as historical references. I have left the awkward bits documented rather than pretending the history was cleaner than it actually was.

## 2.6.1 — 2026-08-30

[GitHub release](https://github.com/ewanc26/inkwell/releases/tag/v2.6.1)

This one is mostly fixes, but they are fairly important ones.

The iOS Writer was accidentally not using the active signed-in session, which meant you could be logged into Inkwell and still be unable to load your publications or publish anything. That is now fixed.

Android sign-in was also sending the OAuth scope incorrectly. It now sends the scope in the format authorization servers actually expect, and the launcher icon has been adjusted so it does not look cramped or clipped across adaptive, round, legacy, and Android 13 themed icon masks.

- Fixed the iOS Writer using the wrong authentication context.
- Fixed Android OAuth scope formatting.
- Fixed the Android launcher icon across the different icon masks.

Historical platform tags: `ios-v2.6.1` and `android-v2.6.1`.

## 2.6.0 — 2026-08-29

[GitHub release](https://github.com/ewanc26/inkwell/releases/tag/v2.6.0)

This release was mostly about making subscriptions feel like they are actually live rather than something you have to keep poking with refresh.

Subscribed documents can now appear in the in-app notification history and trigger local notifications while Inkwell is open. Android also resolves each publication from the PDS that actually owns it instead of assuming everything lives in one place, which matters quite a lot on AT Protocol.

Live repository events are filtered against the publications you actually follow before they are allowed to touch the feed or notification history. I also cleaned up TalkBack labels and actions around reader cards, notifications, and the unread-notification control.

Historical platform tags: `ios-v2.6.0` and `android-v2.6.0`.

## 2.5.0 — 2026-08-28

[GitHub release](https://github.com/ewanc26/inkwell/releases/tag/v2.5.0)

2.5.0 is where the iOS and Android apps started feeling much more like the same application instead of two implementations that happened to read the same records.

Both platforms gained cache-first feeds with shared Jetstream-backed live updates, publication-aware reading, account profiles, reporting, moderation controls, offline document caching, and queued actions that can replay once connectivity comes back.

Discover can search publications and AT Protocol accounts separately. Publication detail pages and account profiles are now proper first-class screens, and report actions are available from feeds, profiles, and content instead of being buried somewhere else.

Moderation also got considerably less hand-wavy: labeler preferences, keyword filters, moderation labels, and explicit reveal controls are all represented in the UI. Cached content now keeps its publication themes and moderation state too, rather than looking different just because the network disappeared.

There were also a lot of smaller fixes around navigation, DPoP requests, Android lint compatibility, accessibility announcements, reduced motion, and feed responsiveness. Most of that work is not individually flashy, but it makes the app much less annoying to actually use.

Historical platform tags: `ios-v2.5.0` and `android-v2.5.0`.

## 2.4.0 — 2026-08-26

[GitHub release](https://github.com/ewanc26/inkwell/releases/tag/v2.4.0)

This release added native AT Protocol reporting on both platforms through `com.atproto.moderation.createReport`.

It also introduced the optional `uk.ewancroft.inkwell.user` declaration record. That gives people a way to publicly say that they use Inkwell, and the website can discover those declarations through Constellation.

The website got a couple of less glamorous fixes at the same time: the server-rendered user carousel now receives its data properly, and the homepage/header logo proportions no longer look squashed.

Historical platform tags: `ios-v2.4.0` and `android-v2.4.0`.

## 2.3.0 — 2026-08-25

[GitHub release](https://github.com/ewanc26/inkwell/releases/tag/v2.3.0)

2.3.0 was mainly about giving people more control over how Inkwell looks and behaves.

Accessibility settings now cover text size, bold text, increased contrast, and underlined links. Appearance settings cover the accent colour, reading font, and light/dark/system themes. None of that is locked behind a licence key; customising the app may show a one-time support prompt, but the settings themselves are just settings.

This release also brought the release tooling into better shape, added CI coverage for the shared Kotlin Multiplatform tests and F-Droid mirror, restored iOS mute/block management, and fixed Android verification for documents whose site association is expressed as an AT URI.

The AGPL App Store Distribution Exception was added here as well, so the licensing actually reflects the distribution options I may want to use later without weakening the rest of the AGPL.

Historical platform tags: `ios-v2.3.0` and `android-v2.3.0`.

## 2.2.0 — 2026-08-21

[GitHub release](https://github.com/ewanc26/inkwell/releases/tag/v2.2.0)

This was the first proper settings-and-notifications release.

Inkwell gained a real Settings screen, notification controls, a shortcut into the system notification settings, and an in-app notification history that can be reviewed or cleared. Tapping a notification opens the relevant content, and Android keeps its unread count in sync.

The same release also added the first version of the accessibility and appearance controls that were expanded in 2.3.0. Legal and About pages were pulled into the same settings home, and Android now renders the Privacy Policy and Terms natively from the same generated source used by iOS and the website.

Historical platform tags: `ios-v2.2.0` and `android-v2.2.0`.

## 2.1.1 — 2026-08-21

[GitHub release](https://github.com/ewanc26/inkwell/releases/tag/v2.1.1)

This was an iOS-only documentation rebuild. No exciting feature story here; I had stale and incomplete legal text in the app and wanted it corrected properly.

The Privacy Policy and Terms were updated to reflect the actual iOS and Android versions, notification behaviour, Keychain restoration from encrypted backups, use of the Bluesky CDN/public API, public feedback records, Vercel hosting, and support links.

I also added the bits that should have been explicit already: UK GDPR controller details, legal bases, retention, transfers, data rights, ICO information, consumer-law carve-outs, content ownership, minimum age, termination, governing law, and the AGPL precedence wording.

Historical platform tag: `ios-v2.1.1`.

## 2.1.0 — 2026-08-21

[GitHub release](https://github.com/ewanc26/inkwell/releases/tag/v2.1.0)

This release added the real-data testing mode I use for release screenshots.

Testing mode keeps your normal signed-in session and performs real network reads, but intercepts every write and puts a clear testing notice on screen. That means I can capture screenshots from the actual app and actual data without accidentally publishing, subscribing, recommending, or changing anything while doing it.

Credits also gained a Bluesky supporters section, and the screen can now be opened while signed out or when no publications are available. A couple of broken public API calls and the developer row were cleaned up at the same time.

Historical platform tags: `ios-v2.1.0` and `android-v2.1.0`.

## 2.0.0 — 2026-08-18

[GitHub release](https://github.com/ewanc26/inkwell/releases/tag/v2.0.0)

2.0.0 is the point where I would consider the two native apps to have the same basic shape.

Discover became a permanent iOS tab alongside Read and Write, matching Android. iOS reader cards gained author names, Discover rows gained publication descriptions, format tags, and subscription state, and both writers got formatting controls, previews, and loss reporting from the shared-core work.

I also dropped the iOS deployment target from iOS 26 to iOS 18. The app does not need to demand a brand-new OS just because I happened to develop it on one; the couple of newer visual effects now have sensible fallbacks.

A fair bit of source-file splitting happened here too. That was deliberately boring refactoring: oversized iOS and Android files were broken into focused modules without trying to sneak behaviour changes into the same work.

On the bug side, the iOS image-picker formatting action finally became an actual action, the missing SF Symbol in the editing banner was replaced, and Android's feedback dialog now resets after submission instead of permanently living on the thank-you screen.

Historical platform tags: `ios-v2.0.0` and `android-v2.0.0`.

## 1.3.1 — 2026-08-16

**Android only.**

A small Android follow-up release after the repositories were consolidated into the Inkwell monorepo.

The source-code URLs were corrected, the signed APK was rebuilt, the self-hosted F-Droid metadata was regenerated, and duplicate historical F-Droid entries were cleaned up.

The GitHub release was created retroactively. The attached APK is the original artefact distributed through Inkwell's F-Droid repository; because the old Android history was squash-imported, the tag points at the earliest Android commit that exists in the monorepo rather than pretending I can reconstruct a commit that is no longer there.

Historical platform tag: `android-v1.3.1`.

## 1.3.0 — 2026-08-16

**Android only.**

This was a short-lived Android release that brought the post/feed-card styling closer to iOS, including colours, chevrons, outlines, and the splash screen. It was published through the self-hosted F-Droid repository and then replaced by 1.3.1 later the same day.

The GitHub release was created retroactively using the original APK preserved in the F-Droid history.

Historical platform tag: `android-v1.3.0`.

## 1.2.0 — 2026-08-16

**Android only.**

This release got the early Android app back into a healthier state: reader/writer compilation errors were fixed, the ViewModels and data layer were tightened up, navigation and Discover were improved, and the reader/feed-card styling moved closer to iOS.

It also added the release screenshots and pushed another signed APK through the self-hosted F-Droid repository.

The GitHub release and tag were backfilled later because the original Android history had already been squash-imported.

Historical platform tag: `android-v1.2.0`.

## 1.1.0 — 2026-08-16

**Android only.**

This is where the Android reader became substantially more than a basic document viewer.

It gained facets, comments, previous/next navigation, duplicate removal, and more Leaflet block types. The Writer gained format-aware publishing for Leaflet, Markpub, pckt, and Offprint content, together with the associated comment/content wire models.

The release was distributed through the self-hosted F-Droid repository. Its GitHub release and tag were added later after the monorepo migration.

Historical platform tag: `android-v1.1.0`.

## 1.0.1 — 2026-08-15

**Android only.**

A small Android patch release.

Publication search was decoding the real v2 API response incorrectly, and a `null` result total could take the whole response down with it. Both are fixed here, alongside the updated F-Droid artefact and legal version references.

The GitHub release and tag were added retroactively after the Android history was squash-imported.

Historical platform tag: `android-v1.0.1`.

## 1.0.0 — 2026-08-15–16

[GitHub release](https://github.com/ewanc26/inkwell/releases/tag/v1.0.0)

This is the first stable release of Inkwell's native apps.

### iOS

The iOS app shipped with the Standard.site reader, Discover, and Writer flows in place. The main ugly bug fixed for release was OAuth token exchange against PDS providers that do not expose DPoP nonces on a `GET`. Inkwell now pre-flights the token endpoint with a sanitised `POST`, obtains the nonce, and only then presents the real authorization code.

Login, Discover, the reader, themes, and navigation also got their release polish here.

The iOS GitHub history was backfilled when the old platform-specific tags were introduced. The attached IPA came from the live AltStore URL shared by the build 49 and build 50 entries, so I cannot honestly claim that artefact is independently verifiable as build 50 specifically.

### Android

The first Android release shipped the native Standard.site reader and Writer with OAuth sign-in, publication discovery, reading/publishing, and the initial self-hosted F-Droid distribution.

The Android GitHub history was also reconstructed later. Its earlier repository history was squash-imported into the monorepo, so the historical tag necessarily points at the earliest Android commit I still have rather than the exact original release commit.

Historical platform tags: `ios-v1.0.0` and `android-v1.0.0`.

## Tagged development history

Before the stable releases, versioning was considerably more chaotic. The iOS and Android projects were still separate, version numbers were occasionally reused, and I also made date-based tags while things were moving quickly.

I am keeping these here as development snapshots rather than trying to rewrite them into a fake single semantic-version history.

### 0.16.0 — 2026-06-22

Added blob-backed Leaflet pages and comment UI to the iOS reader. Also tagged `v2026-06-22`.

### 0.15.1 — 2026-06-22

Fixed Sendable conformance and a Swift type-checker timeout that prevented the previous build compiling cleanly.

### 0.15.0 — 2026-06-22

Threaded lossy facet tracking through the converter so format changes can say what they are unable to round-trip.

### 0.14.0 — 2026-06-22

Added `pub.leaflet.comment` models, lexicon registration, and CRUD support.

### 0.13.0 — 2026-06-22

Added image-blob round-tripping through the Writer.

### 0.12.0 — 2026-06-22

Brought the reader and Writer closer to standard.horse behaviour for Leaflet, pckt, and Offprint content.

### 0.11.8 — 2026-06-22

Replaced regex literals with equivalents supported by the Swift toolchain I actually target.

### 0.11.7 — 2026-06-22

Matched standard.horse ordered-list parsing and added lossy facet tracking.

### 0.11.6 — 2026-06-22

Fixed Swift actor-isolation failures in the following-feed task group.

### 0.11.5 — 2026-06-22

Loaded the following feed in parallel, with an eight-second timeout for each remote PDS.

### 0.11.4 — 2026-06-22

Stopped reserving cover-image space when there is no image, or when loading it fails.

### 0.11.3 — 2026-06-22

Used unauthenticated `listRecords` requests for public records to avoid unnecessary DPoP nonce failures.

### 0.11.2 — 2026-06-22

Added `createdAt` to publication records and made Writer errors less useless.

### 0.11.1 — 2026-06-22

Restored the `atproto` OAuth scope after the granular-scope experiment broke authentication against real providers.

### 0.11.0 — 2026-06-22

Tried replacing the blanket OAuth scope with granular repository/blob scopes. 0.11.1 reverted the important part once provider compatibility made the problem obvious.

### 0.10.6 — 2026-06-22

Combined the Swift actor-isolation and RFC 8252 redirect fixes into a build that actually compiled.

### 0.10.5 — 2026-06-22

Moved OAuth to an RFC 8252 reverse-DNS redirect URI.

### 0.10.4 — 2026-06-22

Fixed actor isolation while constructing login storage.

### 0.10.3 — 2026-06-22

Replaced the placeholder Privacy Policy date with a real one.

### 0.10.2 — 2026-06-22

Corrected the OAuth client-metadata URL to `inkwell.ewancroft.uk`.

### 0.10.1 — 2026-06-21

Improved Dynamic Type support and stopped a few layouts overflowing. Also tagged `v2026-06-21`.

### 0.10.0 — 2026-06-21

Added subscription/recommendation actions, Discover thumbnails, and more overflow protection.

### 0.9.0 — 2026-06-20

Added the Privacy Policy and Terms of Service. Also tagged `v2026-06-20`.

### 0.8.4 — 2026-06-20

Corrected the wordmark colour so it renders properly in Display P3.

### 0.8.3 — 2026-06-20

Applied the exact Display P3 ink-drop colour to the shared Inkwell mark.

### 0.8.2 — 2026-06-20

Restored the missing login icon and deleted the legacy icon assets.

### 0.8.1 — 2026-06-20

Made the tinted app icon more legible.

### 0.8.0 — 2026-06-20

Added subscription notifications and refresh support.

### 0.7.0 — 2026-06-20

Added the first real Standard.site reader and Writer flows.

### 0.6.0 — 2026-06-20

Added the initial Standard.site content support.

### 0.5.1 — 2026-06-19

Fixed asynchronous configuration loading. Also tagged `v2026-06-19`.

### 0.5.0 — 2026-06-19

Added account persistence.

### 0.4.0 — 2026-06-19

Added the first tab-based app structure.

### 0.3.0 — 2026-06-19

Replaced the template screen with Inkwell's first signed-in home view.

### 0.2.0 — 2026-06-19

Added the branded launch screen and fixed dark-mode / Interface Builder problems.

### 0.1.1 — 2026-06-19

Switched identity resolution to Slingshot and fixed a race in the authentication timeout.

### 0.1.0 — 2026-06-25

Early Android preview: corrected the configured site URL from `inkwell.app` to `inkwell.ewancroft.uk`.

### Date-tagged snapshots

- `v2026-06-29`: Android publication creation, cross-platform branding work, PDS endpoint fixes, and subscription-cache invalidation.
- `v2026-06-27`: migrated the early Android app from app-password auth to OAuth 2.1 + DPoP, resolved Bluesky profiles in reader cards, and added the AltStore source listing.
- `v2026-06-25`: made the early Android project buildable in Android Studio and expanded the source documentation.
- `v2026-06-24`: iOS reader pagination, embeds, Constellation discovery, theme consistency, App Intents, retries, accessibility work, and the first native Android client.
- `v2026-06-23`: expanded iOS Leaflet rendering, enabled signed-out browsing, supplemented the following feed from search, and fixed navigation/touch handling.
- `v2026-06-22`: alias of `v0.16.0`.
- `v2026-06-21`: alias of `v0.10.1`.
- `v2026-06-20`: alias of `v0.9.0`.
- `v2026-06-19`: alias of `v0.5.1`.
