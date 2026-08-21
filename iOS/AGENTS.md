# AGENTS.md

Guidance for agents working on Inkwell for iOS, the primary SwiftUI client in the Inkwell monorepo. This directory contains the iOS app, tests, AltStore metadata, and OAuth configuration.

## Principles

1. **KMP-first for all shared logic** — any business logic, data transformation, format conversion, markdown parsing, facet handling, content model, or network orchestration that is not inherently iOS-specific MUST live in the `shared/` KMP module (Kotlin). iOS consumes it via thin Swift wrappers in `SharedKMP.swift` over the `InkwellShared.xcframework`. Before writing new logic in Swift, verify it cannot go in shared KMP. If it can, it must. SwiftUI views, Keychain access, and iOS system integrations are the only things that stay native.
2. **Platform fidelity first** — SwiftUI lifecycle, Keychain security, and iOS human interface guidelines govern every UI decision. Don't port Android UI patterns here.
3. **Protocol truth** — wire formats, record schemas, and OAuth flows come from the atproto spec and upstream references, not inference. Update README claims when the code diverges.
4. **Honest stubs** — unimplemented code returns errors or shows explicit UI placeholders. Never silent no-ops.
5. **Security stays in Keychain** — OAuth tokens, DPoP private keys, PKCE state, and session secrets never leave Keychain or enter logs.
6. **No duplication** — never reimplement logic that already exists in shared KMP. Swift must only contain thin wrappers that call through to the XCFramework. Two independent implementations of the same rule silently drift apart.
7. **Modular file structure** — every file owns one clear responsibility. Each feature lives in its own folder under `Features/` with its ViewModel, View, and helpers as separate files (see `Features/Writer/` as the exemplar). Reusable UI components (toolbars, pickers) get their own files. Keep files under ~400 lines; split before exceeding. `LoginStateManager.swift` and `ContentProvider.swift` were split in 2.0 — the former into `LoginStateManager+*.swift` extensions by concern, the latter into one provider file per content format behind `ProviderRegistry`. Add new XRPC methods to the matching extension and new formats as their own provider.

For monorepo-wide rules, see [`../AGENTS.md`](../AGENTS.md). For Android-specific boundaries, see [`../Android/AGENTS.md`](../Android/AGENTS.md). For website/legal accuracy, see [`../website/AGENTS.md`](../website/AGENTS.md).

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Read First and Source Boundaries

- Read `../README.md`, `../AGENTS.md`, `Inkwell.xcodeproj` project settings, `oauth/client-metadata.json`, privacy/entitlement files, and all source in the touched flow.
- `.letta/worktrees/` contains local shadow checkouts and must not be edited as product source.
- **Shared KMP first:** before writing any business logic in Swift, check whether it already exists or should exist in `shared/src/commonMain/`. Format conversion, markdown parsing, facet handling, content models, and data transformations belong in shared KMP. `SharedKMP.swift` is the thin Swift wrapper that exposes shared logic to the iOS app.
- **Modular structure:** each feature lives in its own folder under `Features/` with ViewModel, View, and helpers as separate files. `Features/Writer/` is the exemplar — `WriterViewModel.swift` (state and business logic), `WriteView.swift` (SwiftUI layout), and `FormattingToolbar.swift` (reusable UI component). Follow this pattern for all new features. Reusable UI components get their own files. Keep files under ~400 lines.
- `Inkwell/Authentication/LoginStateManager.swift` is the central boundary for OAuth/DPoP, PDS resolution, public and authenticated XRPC, records, blobs, subscriptions, recommends, Leaflet comments, profiles, and caches. The core is thin; the work lives in `LoginStateManager+Auth/XRPC/Records/Blobs/Documents/Publications/Subscriptions/Recommends/Comments/Profile/Feedback.swift`. Add to the matching extension, not the core. Note Swift `private` does not cross file boundaries even between extensions of one type — members shared across these files must be `internal`.
- `Inkwell/Protocols/StandardSite/` and `Inkwell/Protocols/ContentFormats/` define tolerant wire models and association/verification rules. `Inkwell/Protocols/ContentFormats/PcktContent.swift` and `OffprintContent.swift` mirror the authoritative pckt.blog and offprint.app lexicons.
- `Inkwell/SharedKMP.swift` is the thin Swift wrapper around the Kotlin Multiplatform shared core (`InkwellShared.xcframework`), exposing `parseAtUri`, `parseMarkdown`, `serializeMarkdown`, `facetsToMarkdown`, `markdownToFacets`, `resolveReaderTheme`, `shouldShowTip`, notification policy helpers, verification URL builders (`sharedPublicationVerificationURL`, `sharedDocumentCanonicalURL`, `sharedDiscoveryLinkTag`, `sharedContainsDocumentLink`), constellation pagination (`sharedPaginateBacklinks`, `deduplicateBacklinks`), URL utilities (`normalizedSite`, `canonicalUrl`), and neutral model mappers (`SharedModelMappers.swift`) for theme types, blob refs, and strong refs.
- `Inkwell/Rendering/` handles Markpub Markdown, Leaflet block/blob pages, pckt, Offprint, Bluesky embeds, polls, and themes. Markdown parsing, serialization, and facet conversion are delegated to the shared KMP via `SharedKMP.swift`; the native `MarkdownParserEngine`/`MarkdownSerializerEngine` in `ContentProvider.swift` remain as thin editor round-trip wrappers. Constellation pagination deduplication and verification link scanning are also delegated to shared KMP.
- `Inkwell/Features/` owns Read/Discover/Write and background subscription polling.
- `InkwellTests/` holds two focused unit sources — `StandardSiteTests.swift` (nine tests) and `BSkyListModelsTests.swift` (three) — twelve tests total, not end-to-end OAuth/editor/rendering coverage. Neither exercises the shared KMP core behind `SharedKMP.swift`; those tests live in `../shared/src/commonTest/` and run through Gradle.

## OAuth and Data Invariants

- OAuth tokens and the P-256 DPoP private key belong in Keychain. `UserDefaults` stores non-secret handle/PDS hints plus notification state and seen URIs; never move credentials, tokens, auth codes, or proof material there or into logs.
- Keep client ID, custom callback scheme, scopes, metadata hosted at `inkwell.ewancroft.uk`, Info.plist URL type, and runtime credentials identical. The hosted metadata at `inkwell.ewancroft.uk` must stay aligned with the checked-in `oauth/client-metadata.json`.
- Preserve issuer/subject/PDS validation, PKCE/authenticator state, DPoP key continuity, nonce retry behavior, refresh rotation, and logout deletion.
- Comment creation writes `pub.leaflet.comment`. Verify authorization for every mutation and ensure the hosted metadata declares this scope. See `../website/AGENTS.md` for OAuth contract details shared with the website and Android.

## Content, Concurrency, and Lifecycle

- AT Protocol open unions and unknown/malformed records must degrade without corrupting valid siblings. Always retain portable `textContent`; preserve format-specific Leaflet/pckt/Offprint/Markpub data unless the user explicitly converts it.
- Facet byte offsets are UTF-8 offsets, not Swift character indices. Blob MIME/size/ref, record `$type`, `site`, `path`, timestamps, themes, and publication association must round-trip exactly.
- `LoginStateManager`, notification/background managers, and UI state are `@MainActor`; network calls are async but much orchestration still runs on the main actor. Do not introduce blocking I/O, shared DPoP nonce races, detached unsafe mutation, or uncancelled view tasks.
- Background refresh identifiers, Info.plist permitted identifiers, scheduling, expiry handlers, notification permission, first-poll baseline, 50-item display retention, and 500-URI seen retention form one contract. Test cold launch, sign-out, denial, expiration, and concurrent foreground fetches.

## Build, Tests, and Distribution

- The checked-in project uses Swift 5 mode, app and test deployment target iOS 18.0, bundle `uk.ewancroft.Inkwell`, marketing version `2.0.0`, and build `52`. The deployment target is kept as low as the SwiftUI APIs actually used allow — iOS 26+-only APIs (`tabBarMinimizeBehavior`, `safeAreaBar`) are gated behind `if #available(iOS 26.0, *)` with a pre-26 fallback (`safeAreaInset`, or simply omitting the behavior) rather than raising the floor. Check `@available`/`#available` before adding any new API newer than 18.0. Resolve Swift packages through Xcode and build the `Inkwell` scheme on an installed compatible simulator.
- **Building requires Xcode 26+ regardless of the 18.0 deployment target.** The app icon is `Inkwell/Inkwell.icon`, an Xcode 26 Icon Composer bundle (SVG layers + `icon.json`), not a classic `Assets.xcassets` appiconset. Older Xcodes don't recognize `.icon` as an asset-catalog input at all and silently compile `Assets.xcassets` alone — which has no icon set named "Inkwell" — producing a hard build error ("None of the input catalogs contained a matching app icon set"), not a warning. If you see that error, you're on the wrong Xcode version, not missing an asset.
- Run `xcodebuild -project Inkwell.xcodeproj -scheme Inkwell -destination 'platform=iOS Simulator,name=<available iOS 18+ device>' build test`, adapting only the destination to installed runtimes. Inspect failures from ATProtoKit, OAuthenticator, and ATResolve resolution separately. `InkwellUITests` has no source files (pre-existing) and fails to load its bundle if run — pass `-skip-testing:InkwellUITests` or target `InkwellTests` directly.
- **Testing mode:** `-testing` keeps the real session and real reads while intercepting every write, surfacing a root-level "Testing mode" alert instead. Enforced in `App/TestingMode.swift` and the `LoginStateManager` write choke points (`createRecord`/`updateRecord`/`deleteRecord`/`uploadBlob`, plus `submitFeedback`), which throw `LoginError.testingMode`. No mock data, no fake session — screenshot capture requires signing in. Deep-link a tab with `-tab-reader`/`-tab-discover`/`-tab-writer`.
- Unit tests cover the Inkwell NSID namespace, AT-URI rejection of malformed values, association/canonical URLs, verification endpoint paths, wire keys, search v2 decoding, notification JSON, and tolerant record pages (nine tests in `InkwellTests/StandardSiteTests.swift`), plus `app.bsky.graph.getList` decoding and the duplicated supporters-list AT-URI (three tests in `InkwellTests/BSkyListModelsTests.swift`, mirroring Android's `BlueskyListModelsTest`).
- **The shared core is not covered by `xcodebuild ... test`.** `SharedKMP.swift` calls into a prebuilt `InkwellShared.xcframework`; its 135 tests live in `../shared/src/commonTest/` and only run via Gradle from `../Android`: `./gradlew :shared:jvmTest` (or `:shared:allTests` for the Kotlin/Native iOS targets, much slower). If a change touches shared logic — markdown, facets, content conversion, reader themes, verification URLs, tip/notification policy — run that task and rebuild the XCFramework; a green iOS test run proves nothing about it.
- Manually exercise fresh/cancelled OAuth, bad state/issuer/nonce, restore/refresh/revocation/logout, every reader format, Unicode facets, blobs, create/edit/delete, subscriptions/recommends/comments, verification, pagination, offline errors, and background/local notifications.
- `altstore/source.json` must match bundle/version/build, privacy/permissions, hosted icon/IPA, byte size, and release notes. Screenshots are hosted at `https://inkwell.ewancroft.uk/screenshots/ios/`.
- Never commit IPA archives, signing profiles, DerivedData, xcuserdata/UI state, or real credentials.

## Things that look wrong but are not

- **iOS `UserDefaults` stores only non-secret hints** (handle/PDS hints, notification state, seen URIs) — credentials and proof material stay in Keychain.
- **`.letta/worktrees/` exists but is not product source** — it contains local shadow checkouts and must not be edited.
