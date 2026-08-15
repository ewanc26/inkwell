# AGENTS.md

Guidance for agents working on Inkwell, a native reader and writer for the Standard.site publishing ecosystem on AT Protocol. This monorepo contains both the iOS (SwiftUI) and Android (Kotlin/Compose) clients.

## Read First and Source Boundaries

- Read `README.md`, platform-specific build files, and all source in the touched flow.
- **iOS:** `iOS/` is authoritative; `.letta/worktrees/` contains local shadow checkouts and must not be edited as product source.
  - `Authentication/LoginStateManager.swift` is the large central boundary for OAuth/DPoP, PDS resolution, public and authenticated XRPC, records, blobs, subscriptions, recommends, Leaflet comments, profiles, and caches.
  - `Protocols/StandardSite/` and `Protocols/ContentFormats/` define tolerant wire models and association/verification rules. `Rendering/` handles Markpub Markdown, Leaflet block/blob pages, pckt, Offprint, Bluesky embeds, polls, and themes.
  - `Features/` owns Read/Discover/Write and background subscription polling. `InkwellTests/StandardSiteTests.swift` is a focused unit suite, not end-to-end OAuth/editor/rendering coverage.
- **Android:** `Android/` is the Android client. Key boundaries:
  - `data/repository/PdsRepository.kt` performs public/authenticated XRPC, records, blobs, subscriptions, recommends, and comments.
  - `data/model` defines partial Standard.site/Leaflet shapes. `ConstellationClient` queries backlinks.
  - Hilt modules construct OAuth/network services. ViewModels own `StateFlow`; Compose screens and `NavGraph` own UI/navigation.
  - `data/remote/StandardSiteVerifier.kt` implements publication/document verification. There is no Worker or notification manager despite README claims.

## OAuth and Data Invariants

- **iOS:** OAuth tokens and the P-256 DPoP private key belong in Keychain. `UserDefaults` stores non-secret handle/PDS hints plus notification state and seen URIs; never move credentials, tokens, auth codes, or proof material there or into logs. Keep client ID, custom callback scheme, scopes, metadata hosted at `inkwell.ewancroft.uk`, Info.plist URL type, and runtime credentials identical. Preserve issuer/subject/PDS validation, PKCE/authenticator state, DPoP key continuity, nonce retry behavior, refresh rotation, and logout deletion.
- **Android:** OAuth sessions are JSON in `EncryptedSharedPreferences` backed by a MasterKey. Keep tokens, refresh state, PKCE/DPoP material, and authorization URLs out of logs and ordinary preferences. The manifest accepts every URI using the custom scheme, while `MainActivity` checks `/callback`. Preserve state validation inside the OAuth library, reject unrelated/deceptive callbacks, and test cold/warm `singleTask` delivery. Authentication changes do not automatically rebuild an existing Navigation Compose graph merely because `startDestination` changes.
- **Both:** Keep handles, DIDs, PDS origins, AT URIs, CIDs, rkeys, revisions, canonical site URLs, and verification proofs distinct. Public cross-repo reads must resolve the owning DID's PDS rather than assume the signed-in service.

## Content, Concurrency, and Lifecycle

- AT Protocol open unions and unknown/malformed records must degrade without corrupting valid siblings. Always retain portable `textContent`; preserve format-specific Leaflet/pckt/Offprint/Markpub data unless the user explicitly converts it.
- Facet byte offsets are UTF-8 offsets, not platform character indices. Blob MIME/size/ref, record `$type`, `site`, `path`, timestamps, themes, and publication association must round-trip exactly.
- **iOS:** `LoginStateManager`, notification/background managers, and UI state are `@MainActor`; network calls are async but much orchestration still runs on the main actor. Do not introduce blocking I/O, shared DPoP nonce races, detached unsafe mutation, or uncancelled view tasks. Background refresh identifiers, Info.plist permitted identifiers, scheduling, expiry handlers, notification permission, first-poll baseline, 50-item display retention, and 500-URI seen retention form one contract.
- **Android:** Compose state must remain lifecycle-aware and process-recreatable. Avoid writes in composition/effects, duplicate OAuth callbacks, stale concurrent feed loads, and uncancelled requests. Validate back stack, rotation, process death, deep links, and logout. Use structured concurrency and `Dispatchers.IO` for synchronous OkHttp. URL-encode every XRPC query value.
- **Both:** Preserve native accessibility, Dynamic Type, dark/light behavior, reduced motion, safe-area behavior, and platform-appropriate mark/wordmark coordinates.

## Build, Tests, and Distribution

- **iOS:** The checked-in project uses Swift 5 mode, app deployment target iOS 26.0, test target iOS 26.5, bundle `uk.ewancroft.Inkwell`, marketing version `1.0`, and build `49`. Resolve Swift packages through Xcode and build the `Inkwell` scheme on an installed compatible simulator.
  - Run `xcodebuild -project Inkwell.xcodeproj -scheme Inkwell -destination 'platform=iOS Simulator,name=<available iOS 26.5 device>' build test`, adapting only the destination to installed runtimes.
  - Unit tests cover AT-URI parsing, association/canonical URLs, verification endpoint paths, wire keys, search decoding, notification JSON, and tolerant record pages.
  - `altstore/source.json` must match bundle/version/build, privacy/permissions, hosted icon/IPA, byte size, and release notes.
- **Android:** Target facts: compile/target SDK 36, minimum SDK 26, Java/Kotlin JVM 17, release minification enabled, app ID `uk.ewancroft.inkwell`, and debug ID suffix `.debug`.
  - Run `./gradlew clean assembleDebug lint test` from `Android/` with a valid local Android SDK/JDK. For release work also run `./gradlew assembleRelease` and inspect R8 output.
  - There are no JVM or instrumentation tests for the app in general. `StandardSiteVerifierTest` is the only unit test source, including three tests that hit the real `blog.ewancroft.uk` standard.site publication over the network.
- **Both:** Manually exercise fresh/cancelled OAuth, bad state/issuer/nonce, restore/refresh/revocation/logout, every reader format, Unicode facets, blobs, create/edit/delete, subscriptions/recommends/comments, verification, pagination, offline errors, and background/local notifications.
- Never commit `local.properties`, `.idea/`, `.gradle/`, `app/build/`, signing material, OAuth sessions, or real credentials.
