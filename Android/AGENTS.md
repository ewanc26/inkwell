# AGENTS.md

Guidance for agents working on the Android Inkwell client. It is a Kotlin/Compose counterpart to iOS Inkwell.

## Principles

1. **KMP-first for all shared logic** — Android is the owner and primary consumer of the `shared/` KMP module (Kotlin). Any business logic, data transformation, format conversion, markdown parsing, facet handling, content model, or network orchestration that is not inherently platform-specific MUST be written here first in Kotlin, then consumed by iOS via the `InkwellShared.xcframework`. Compose UI, EncryptedSharedPreferences, and Android system integrations (WorkManager, Hilt) are the only things that stay native. Before writing new logic in `app/`, verify it cannot go in `shared/`. If it can, it must.
2. **Platform fidelity first** — Kotlin/Compose patterns, EncryptedSharedPreferences security, and Material 3 UX govern every UI decision. Don't port iOS lifecycle or security assumptions here.
3. **Protocol truth** — wire formats, record schemas, and OAuth flows come from the atproto spec and upstream references, not inference. Update README claims when the code diverges.
4. **Honest stubs** — unimplemented features show explicit UI banners or return errors. Never silent no-ops or fabricated successes.
5. **Security stays in EncryptedSharedPreferences** — OAuth tokens, refresh state, PKCE/DPoP material, and authorization URLs never enter logs or ordinary preferences.
6. **No duplication** — never reimplement logic that already exists in shared KMP. The Android `app/` module consumes shared logic directly; iOS gets it via XCFramework. Two independent implementations of the same rule silently drift apart.
7. **Modular file structure** — every file owns one clear responsibility. Each feature lives in its own folder under `app/src/main/java/.../ui/` with ViewModel, Screen, and helpers as separate files (see `ui/writer/` as the exemplar). Reusable UI components (toolbars, pickers, rendering views) get their own files. Keep files under ~400 lines; split before exceeding. `PdsRepository.kt` and `PostDetailScreen.kt` were split in 2.0 into `PdsRepository{Documents,Graph,Comments,Polls,Feedback}.kt` and `PostDetail{Body,Comments,Models,ContentParsing,PollExtensions}.kt` — add new XRPC methods and rendering helpers to the matching file.

For monorepo-wide rules, see [`../AGENTS.md`](../AGENTS.md). For iOS-specific boundaries, see [`../iOS/AGENTS.md`](../iOS/AGENTS.md). For website/legal accuracy, see [`../website/AGENTS.md`](../website/AGENTS.md).

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Read First and Source Boundaries

- Read `README.md`, Gradle/version catalog files, `AndroidManifest.xml`, `docs/oauth/client-metadata.json`, and all touched Kotlin. Compare shared wire behavior with the owned iOS `../iOS/` checkout, without copying Swift lifecycle or security assumptions.
- **Shared KMP first:** Android owns and develops the `shared/` KMP module. Any business logic, data transformation, format conversion, markdown parsing, facet handling, content model, or network orchestration that is not inherently platform-specific MUST be written here in Kotlin first, then consumed by iOS via the `InkwellShared.xcframework`. Before writing new logic in `app/`, verify it cannot go in `shared/`. If it can, it must.
- **Modular structure:** each feature lives in its own folder under `app/src/main/java/.../ui/` with ViewModel, Screen, and helpers as separate files. `ui/writer/` is the exemplar — `WriterViewModel.kt` (state and business logic), `WriterScreen.kt` (Compose layout), `FormattingToolbar.kt` (reusable UI component), and `MarkdownConverter.kt` (shared KMP bridge). Follow this pattern for all new features. Reusable UI components get their own files. Keep files under ~400 lines.
- `app/src/main/java/uk/ewancroft/inkwell/data/auth` stores the OAuth session, `app/src/main/java/uk/ewancroft/inkwell/data/repository/PdsRepository.kt` performs public/authenticated XRPC, `app/src/main/java/uk/ewancroft/inkwell/data/model` defines Standard.site/Leaflet/pckt/Offprint shapes, and `app/src/main/java/uk/ewancroft/inkwell/data/remote/ConstellationClient.kt` delegates pagination/deduplication to shared KMP.
- `app/src/main/java/uk/ewancroft/inkwell/data/remote/BSkyPostFetcher.kt` fetches Bluesky posts from the public API for embed rendering.
- Hilt modules construct OAuth/network services. ViewModels own `StateFlow`; Compose screens and `NavGraph` own UI/navigation. `InkwellNotificationManager.kt` and `InkwellNotificationWorker.kt` implement foreground Jetstream-backed local notifications with WorkManager background polling fallback. `app/src/main/java/uk/ewancroft/inkwell/data/remote/StandardSiteVerifier.kt` delegates URL construction and link scanning to shared KMP; networking and caching remain native.
- Target facts: compile/target SDK 36, minimum SDK 26, Java/Kotlin JVM 17, release minification enabled, app ID `uk.ewancroft.inkwell`, debug ID suffix `.debug`, version `2.6.1`, versionCode `14`.

## Current Capability Gaps

- Writer: split-pane-style editor with formatting toolbar, live markdown preview toggle, loss reporting banner, image upload, and document editing with revision support. Format selection passes the chosen format to `MarkdownConverter.convert()` which produces the correct content type (Leaflet, Markpub, pckt, Offprint).
- The detail screen (`PostDetailScreen`/`PostDetailViewModel`) fetches the real document and renders it — Leaflet blocks with rich-text facets via the pre-existing `LeafletBlockRenderer`, Markpub markdown via `MarkdownRendererView` with full block and inline formatting, pckt/Offprint block arrays converted to markdown via `PcktOffprintConverter` (with facet-aware inline formatting), legacy `textContent` as plain paragraphs, and a generic plaintext-leaf walk for any other/unmodelled block-array format so content isn't silently dropped. Comments and interactions (likes, reposts, replies) are implemented.
- Subscribe/unsubscribe (on publications, from Discover) and recommend/unrecommend with a live count (on documents, from the detail screen) are wired end-to-end against real `site.standard.graph.subscription`/`site.standard.graph.recommend` records and Constellation backlink counts.
- Publication/document verification is implemented in `data/remote/StandardSiteVerifier.kt` with in-memory verification-result caching (5-minute TTL). Verification is wired into both `PostDetailViewModel`/`PostDetailScreen` and `ReaderViewModel` feed cards for direct `https://` sites and documents whose `site` is a `site.standard.publication` AT-URI. Feed verification preserves each document path, resolves each unique publication AT-URI once per verification batch, caches successful publication-record resolutions for five minutes, and also runs on appended Following pages. A publication-resolution failure leaves the card unconfirmed rather than treating the network/PDS lookup failure as failed domain verification.
- App-level test coverage is two files and no instrumentation tests: `StandardSiteVerifierTest` (13 tests, added alongside the verifier — three of them hit the real `blog.ewancroft.uk` standard.site publication over the network and fail offline) and `data/model/common/SearchModelsTest` (2 tests, pub-search wire decoding). `./gradlew test` otherwise executes effectively empty tasks for the rest of the app; never report that as behavioral coverage beyond these two files. The repo's real automated coverage is in the shared KMP module — see Build, Tests, and Distribution.

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
- Manually test OAuth success/cancel/state/error/restore/logout; navigation after callback; malformed/expired encrypted storage; cross-PDS reads; initial and paginated feed verification for both direct HTTPS and AT-URI publication sites; publication-resolution/status/network failures; actual document records; Unicode/blob/theme decoding; rotation/process death; large fonts; and TalkBack.

## Build, Tests, and Distribution

- **Testing mode:** launch with `--ez testing true` (optionally `--es tab reader|discover|writer`) to keep the real session and real reads while intercepting every write, which raises a "Testing mode" dialog above the nav host. Enforced in `TestingConfig.kt` and the four `PdsRepository` write choke points, which throw `TestingModeException`; `submitFeedback` routes through `createRecord` so it's covered. No mock data and no fake auth — `isAuthenticated` is the real auth state only, so capture requires signing in.
- Run `./gradlew clean assembleDebug lint test` with a valid local Android SDK/JDK, then add targeted unit/instrumentation tests for changed behavior. For release-sensitive work also run `./gradlew assembleRelease` and inspect R8 output.
- **Version bump policy:** Only bump the version (`version` in `build.gradle.kts`) and `versionCode` when there are genuine user-visible changes (new features, UI improvements, bug fixes visible to users). For re-releases of the same semver (e.g. rebuilding the APK after a source.json update, fixing a non-functional issue), keep the marketing version the same and only bump `versionCode`. This ensures F-Droid users get the correct build without a new Play Store listing, and prevents premature version proliferation.

- **Regenerate legal docs before building a release APK.** The APK compiles `LegalDocuments.kt`, which quotes the shipping versions; run `node tools/legal/render.mjs` after any version bump or the in-app Privacy/Terms screens ship quoting last release. Before publishing, confirm signature continuity — `apksigner verify --print-certs` must report the same SHA-256 certificate as the previous APK in `fdroid-repo/repo/` (`1a020456…`), or every existing install loses its upgrade path.
- App-level test coverage is two files and no instrumentation tests: `StandardSiteVerifierTest` (13 tests, three of which hit the real `blog.ewancroft.uk` standard.site publication over the network and fail offline) and `SearchModelsTest` (2 tests). `./gradlew test` otherwise executes effectively empty tasks for the rest of the app; never report that as behavioral coverage beyond these two files.
- **`./gradlew test` does not run the shared module's tests.** `settings.gradle.kts` includes `:shared` (mapped to `../shared`), and that module holds the bulk of the repo's automated coverage — 135 tests across ten files in `shared/src/commonTest/`, covering AT-URI parsing, markdown parsing and inline scanning, facet schema/conversion, content-format conversion, URL utilities, reader themes, and the tip-prompt/notification policies. The KMP `jvm()` target exposes `jvmTest`, not `test`, so the aggregate `test` task skips `:shared` silently — a green `test` run says nothing about shared logic.
  - Run `./gradlew :shared:jvmTest` for the JVM target, or `./gradlew :shared:allTests` to include the Kotlin/Native iOS targets (much slower). Any change under `shared/` must be verified with one of these; the iOS `xcodebuild ... test` run won't catch it either, since it consumes a prebuilt `InkwellShared.xcframework`.
- Never commit `local.properties`, `.idea/`, `.gradle/`, `app/build/`, signing material, OAuth sessions, or local `.letta/` data.

## Things that look wrong but are not

- **Android's `ContentUnion` is intentionally incomplete** — it would silently lose unmodelled formats if round-tripped, so the partial set is deliberate.
