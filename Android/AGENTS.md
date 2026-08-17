# AGENTS.md

Guidance for agents working on the Android Inkwell client. It is a Kotlin/Compose counterpart to iOS Inkwell.

## Principles

1. **Platform fidelity first** — Kotlin/Compose patterns, EncryptedSharedPreferences security, and Material 3 UX govern every decision. Don't port iOS lifecycle or security assumptions here.
2. **Protocol truth** — wire formats, record schemas, and OAuth flows come from the atproto spec and upstream references, not inference. Update README claims when the code diverges.
3. **Honest stubs** — unimplemented features show explicit UI banners or return errors. Never silent no-ops or fabricated successes.
4. **Security stays in EncryptedSharedPreferences** — OAuth tokens, refresh state, PKCE/DPoP material, and authorization URLs never enter logs or ordinary preferences.
5. **No duplication** — reuse shared logic from the iOS checkout or website rather than copy-pasting Kotlin equivalents.

For monorepo-wide rules, see [`../AGENTS.md`](../AGENTS.md). For iOS-specific boundaries, see [`../iOS/AGENTS.md`](../iOS/AGENTS.md). For website/legal accuracy, see [`../website/AGENTS.md`](../website/AGENTS.md).

## AI-assisted contributions

AI tools may be used when contributing, but do not add `Co-authored-by:` trailers crediting AI agents. Attribution is reserved for human contributors only.

## Read First and Source Boundaries

- Read `README.md`, Gradle/version catalog files, `AndroidManifest.xml`, `docs/oauth/client-metadata.json`, and all touched Kotlin. Compare shared wire behavior with the owned iOS `../iOS/` checkout, without copying Swift lifecycle or security assumptions.
- `app/src/main/java/uk/ewancroft/inkwell/data/auth` stores the OAuth session, `app/src/main/java/uk/ewancroft/inkwell/data/repository/PdsRepository.kt` performs public/authenticated XRPC, `app/src/main/java/uk/ewancroft/inkwell/data/model` defines partial Standard.site/Leaflet shapes, and `app/src/main/java/uk/ewancroft/inkwell/data/remote/ConstellationClient.kt` queries backlinks.
- `app/src/main/java/uk/ewancroft/inkwell/data/remote/BSkyPostFetcher.kt` fetches Bluesky posts from the public API for embed rendering.
- Hilt modules construct OAuth/network services. ViewModels own `StateFlow`; Compose screens and `NavGraph` own UI/navigation. `InkwellNotificationManager.kt` and `InkwellNotificationWorker.kt` implement WorkManager-based background notification polling. `app/src/main/java/uk/ewancroft/inkwell/data/remote/StandardSiteVerifier.kt` implements publication/document verification.
- Target facts: compile/target SDK 36, minimum SDK 26, Java/Kotlin JVM 17, release minification enabled, app ID `uk.ewancroft.inkwell`, debug ID suffix `.debug`, version `1.3.1`, versionCode `6`.

## Current Capability Gaps

- Writer format selection is functional: `selectedFormat` is passed to `MarkdownConverter.convert()` which produces the correct content type (Leaflet, Markpub, pckt, Offprint). Image upload and blob handling are implemented. Document editing with revision support is functional.
- The detail screen (`PostDetailScreen`/`PostDetailViewModel`) fetches the real document and renders it — Leaflet blocks with rich-text facets via the pre-existing `LeafletBlockRenderer`, Markpub markdown via `MarkdownRendererView` with full block and inline formatting, pckt/Offprint block arrays converted to markdown via `PcktOffprintConverter` (with facet-aware inline formatting), legacy `textContent` as plain paragraphs, and a generic plaintext-leaf walk for any other/unmodelled block-array format so content isn't silently dropped. Comments and interactions (likes, reposts, replies) are implemented.
- Subscribe/unsubscribe (on publications, from Discover) and recommend/unrecommend with a live count (on documents, from the detail screen) are wired end-to-end against real `site.standard.graph.subscription`/`site.standard.graph.recommend` records and Constellation backlink counts.
- Publication/document verification is implemented in `data/remote/StandardSiteVerifier.kt` with in-memory caching (5-minute TTL). Verification is wired into both `PostDetailViewModel`/`PostDetailScreen` (full verification with publication resolution for AT-URI sites) and `ReaderViewModel` feed cards (verification for `https://` site documents only). AT-URI-site documents in the feed are not yet verified because resolving the publication requires an extra network call per document.
- There are no JVM or instrumentation tests for the app in general. `StandardSiteVerifierTest` (added alongside the verifier) is the only unit test source in the project, including three tests that hit the real `blog.ewancroft.uk` standard.site publication over the network — they'll fail offline. `./gradlew test` otherwise executes effectively empty tasks for the rest of the codebase; never report that as behavioral coverage beyond this one file.

## OAuth and Data Invariants

- OAuth sessions are JSON in `EncryptedSharedPreferences` backed by a MasterKey. Keep tokens, refresh state, PKCE/DPoP material, and authorization URLs out of logs and ordinary preferences. Account for deprecated/security-crypto migration before changing storage.
- Runtime scope and `docs/oauth/client-metadata.json` currently align on publication/document/subscription/recommend/blob access and the custom callback. The production file is hosted by another repo/site; update and verify both together. See `../website/AGENTS.md` for OAuth contract details shared with the website.
- The manifest accepts every URI using the custom scheme, while `app/src/main/java/uk/ewancroft/inkwell/MainActivity.kt` checks `/callback`. Preserve state validation inside the OAuth library, reject unrelated/deceptive callbacks, and test cold/warm `singleTask` delivery.
- Authentication changes do not automatically rebuild an existing Navigation Compose graph merely because `startDestination` changes. Verify post-callback and logout navigation explicitly rather than assuming recomposition redirects.
- Use structured concurrency and `Dispatchers.IO` for synchronous OkHttp and JSON deserialization. Do not block the main thread with network or parsing work.
- URL-encode every XRPC query value. Current PDS/Constellation URL strings interpolate DIDs, collections, subjects, sources, limits, and cursors manually; responses are force-unwrapped/decoded without status checks or consistent closing. Harden these boundaries before expanding them.
- Preserve DIDs, AT URIs, CIDs, rkeys, collection NSIDs, blob refs, UTF-8 facet byte offsets, unknown open-union variants, and author-PDS resolution. The current `ContentUnion` is intentionally incomplete and would lose unmodelled formats if round-tripped.

## Content, Concurrency, and Lifecycle

- AT Protocol open unions and unknown/malformed records must degrade without corrupting valid siblings. Always retain portable `textContent`; preserve format-specific Leaflet/pckt/Offprint/Markpub data unless the user explicitly converts it.
- Facet byte offsets are UTF-8 offsets, not platform character indices. Blob MIME/size/ref, record `$type`, `site`, `path`, timestamps, themes, and publication association must round-trip exactly.
- Compose state must remain lifecycle-aware and process-recreatable. Avoid writes in composition/effects, duplicate OAuth callbacks, stale concurrent feed loads, and uncancelled requests. Validate back stack, rotation, process death, deep links, and logout. Use structured concurrency and `Dispatchers.IO` for synchronous OkHttp. URL-encode every XRPC query value.
- Preserve native accessibility, Dynamic Type, dark/light behavior, reduced motion, safe-area behavior, and platform-appropriate mark/wordmark coordinates.

## UI and Validation

- Compose state must remain lifecycle-aware and process-recreatable. Avoid writes in composition/effects, duplicate OAuth callbacks, stale concurrent feed loads, and uncancelled requests. Validate back stack, rotation, process death, deep links, and logout.
- Preserve Material 3 semantics, adaptive light/dark theme, edge-to-edge insets, font scaling, TalkBack labels/order, keyboard/IME actions, RTL, reduced motion, and accessible error feedback. The fixed light splash currently precedes themed content; test dark mode flashes.
- Manually test OAuth success/cancel/state/error/restore/logout; navigation after callback; malformed/expired encrypted storage; cross-PDS reads; pagination/status/network failures; actual document records; Unicode/blob/theme decoding; rotation/process death; large fonts; and TalkBack.

## Build, Tests, and Distribution

- Run `./gradlew clean assembleDebug lint test` with a valid local Android SDK/JDK, then add targeted unit/instrumentation tests for changed behavior. For release-sensitive work also run `./gradlew assembleRelease` and inspect R8 output.
- There are no JVM or instrumentation tests for the app in general. `StandardSiteVerifierTest` is the only unit test source, including three tests that hit the real `blog.ewancroft.uk` standard.site publication over the network — they'll fail offline. `./gradlew test` otherwise executes effectively empty tasks for the rest of the codebase; never report that as behavioral coverage beyond this one file.
- Never commit `local.properties`, `.idea/`, `.gradle/`, `app/build/`, signing material, OAuth sessions, or local `.letta/` data.

## Things that look wrong but are not

- **Android's `ContentUnion` is intentionally incomplete** — it would silently lose unmodelled formats if round-tripped, so the partial set is deliberate.
