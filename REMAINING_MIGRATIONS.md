# Inkwell Shared Core — Remaining Migration Opportunities

_Last updated: 2026-08-18_

This document captures platform-specific backend logic that could still be migrated to the shared KMP module, ranked by impact and feasibility.

## Already Migrated

| Module | Status | Commit |
|--------|--------|--------|
| AT-URI Parser | ✅ | `d547751`, `a1049eb` |
| Markdown Parser/Serializer | ✅ | `eafd39d`, `d7e13d4` |
| Facet Schema Constants | ✅ | `dad33b5`, `f075b9f` |
| Facet Byte-Range ↔ Markdown Converter | ✅ | `e87e385` |
| Reader Theme Resolution | ✅ | earlier shared core wiring |
| Tip-Prompt Gating | ✅ | earlier shared core wiring |
| Notification Retention Policy | ✅ | earlier shared core wiring |
| Verification URL Builders + Link Scanner | ✅ | `69144b0` |
| Constellation Pagination + Deduplication | ✅ | `69144b0` |
| URL Utilities (`normalizedSite`, `canonicalUrl`) | ✅ | `69144b0` |
| Neutral Shared-Model Layer (DTOs) | ✅ | `b1503c8` |
| Content Format Block-Type Mapping | ✅ | shared converters for all 4 formats |

---

## Tier 1 — High Impact, High Feasibility

### 1. Content Format Conversion Block-Type Mapping ✅ COMPLETED

**Shared:** `shared/src/commonMain/kotlin/.../content/` with converters:
- `LeafletContentConverter.kt` — MarkdownBlock ↔ Leaflet JSON (pages/blocks)
- `PcktContentConverter.kt` — MarkdownBlock ↔ pckt JSON (items array)
- `OffprintContentConverter.kt` — MarkdownBlock ↔ Offprint JSON (items array)
- `MarkpubContentConverter.kt` — Markdown ↔ Markpub JSON (identity)
- `ContentFormatDispatcher.kt` — unified dispatch by format name or `$type`
- `BlockLossLabels.kt` — shared loss label maps per format
- `JsonMapBridge.kt` — converts `Map<String, Any?>` ↔ kotlinx.serialization `JsonObject`

**Android:** `MarkdownConverter.kt` reduced from ~390 lines to ~25 lines; `PcktOffprintConverter.kt` reduced from ~360 lines to ~40 lines.
**iOS:** `SharedKMP.swift` gains `sharedContentToMarkdown()`, `sharedMarkdownToContent()`, `sharedBlockLossLabels()` wrappers.
**Tests:** `ContentConverterTest.kt` — 30+ tests covering round-trip for all four formats.

---

### 2. Inline Markdown Rendering (Byte-Range → Attributed String)

**iOS:** `iOS/Inkwell/Rendering/MarkdownRendererView.swift` (`applyInlineFormatting()`, `byteRangeToAttrRange()`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/ui/reader/MarkdownRendererView.kt` (`renderInline()`, `findClosing()`) + `Android/app/src/main/java/uk/ewancroft/inkwell/ui/reader/LeafletBlockRenderer.kt` (`buildAnnotatedString()`, `byteOffsetsToCharRange()`)

**What's duplicated:**
- UTF-8 byte-offset to character-range conversion (identical algorithm)
- Delimiter pair scanning for `**bold**`, `*italic*`, `` `code` ``, `~~strike~~`, `[link](url)`
- Opening/closing delimiter matching with escape handling

**Effort:** Medium (~200 lines of pure string processing)  
**Risk:** Low — pure text processing, no platform dependencies  
**Benefit:** Ensures both platforms render inline formatting identically; fixes diverge less often

---

### 3. Format Count Utility ✅ COMPLETED

**iOS:** `iOS/Inkwell/Rendering/BSkyPostEmbed.swift` (`formatCount(_ count: Int) -> String`) → now uses `sharedFormatCount()` from `SharedKMP.swift`  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/ui/reader/LeafletBlockRenderer.kt` (`formatCount()`)

**What's duplicated:**
- Number abbreviation: `1,500,000` → `"1.5M"`, `2,300` → `"2.3K"`, etc.

**Effort:** Trivial (~5 lines)  
**Risk:** None  
**Benefit:** Eliminates trivial duplication; proves shared utility pattern

---

### 3b. XRPC Endpoints & OAuth Scopes ✅ COMPLETED

**iOS:** `iOS/Inkwell/Authentication/LoginStateManager.swift` — all hardcoded `/xrpc/...` paths and OAuth scope strings replaced with shared KMP wrappers:
- `sharedXrpcServerGetSession()`, `sharedXrpcSyncGetBlob()`, `sharedXrpcRepoCreateRecord()`, `sharedXrpcRepoDeleteRecord()`, `sharedXrpcRepoGetRecord()`, `sharedXrpcRepoListRecords()`
- `sharedOAuthScopeAtproto()`, `sharedOAuthScopeBlobAll()`, `sharedOAuthScopeRepoPublication()`, `sharedOAuthScopeRepoDocument()`, `sharedOAuthScopeRepoSubscription()`, `sharedOAuthScopeRepoRecommend()`

**Additional iOS files migrated:**
- `iOS/Inkwell/Rendering/BSkyProfileFetcher.swift` — `sharedPublicBskyApi()`, `sharedXrpcIdentityResolveHandle()`, `sharedXrpcActorGetProfile()`
- `iOS/Inkwell/Rendering/ConstellationClient.swift` — `sharedConstellationApi()`, `sharedXrpcMicrocosmGetBacklinks()`
- `iOS/Inkwell/Rendering/BSkyPostEmbed.swift` — Bluesky embed types and `sharedXrpcFeedGetPosts()`

---

### 3c. Search Result Classification ✅ COMPLETED

**iOS:** `iOS/Inkwell/Features/Discover/StandardReaderAPI.swift` (`ReaderSearchResult.isPublication`, `isStandardSiteDocument`, `webURL`) → now uses shared KMP wrappers:
- `sharedIsPublication(type:)` — checks `type == "publication"`
- `sharedIsStandardSiteDocument(uri:)` — parses AT-URI and checks collection == `site.standard.document`
- `sharedWebURL(basePath:path:rkey:platform:isPublication:)` — constructs canonical web URL

---

### 3d. Publication Matching ✅ COMPLETED

**New shared KMP:** `shared/src/commonMain/kotlin/uk/ewancroft/inkwell/shared/content/PublicationMatcher.kt` — `documentBelongsToPublication(documentSite, publicationUri, publicationUrl)`

**iOS files migrated:**
- `iOS/Inkwell/Features/Subscriptions/NotificationManager.swift` — uses `sharedDocumentBelongsToPublication()` instead of `PublicationEntry.contains()`
- `iOS/Inkwell/Rendering/StandardSitePostEmbed.swift` — uses shared publication matching
- `iOS/Inkwell/Features/Reader/BrowseDocumentsView.swift` — uses shared publication matching
- `iOS/Inkwell/Protocols/StandardSite/StandardSiteTypes.swift` — removed `PublicationEntry.contains()` extension (dead after migration)

**Android files migrated:**
- `Android/app/src/main/java/uk/ewancroft/inkwell/data/remote/InkwellNotificationManager.kt` — uses `PublicationMatcher.documentBelongsToPublication()` instead of inline matching

---

### 3e. Collection NSIDs ✅ COMPLETED

**iOS files migrated:**
- `iOS/Inkwell/Protocols/StandardSite/SiteStandardComment.swift` — `sharedLeafletComment()`
- `iOS/Inkwell/Rendering/PollEmbedView.swift` — `sharedLeafletPollDefinition()`, `sharedLeafletPollVote()`
- `iOS/Inkwell/Rendering/ConstellationClient.swift` — `sharedLeafletComment()` for backlink source

---

### 3f. Dead Code Removal ✅ COMPLETED

- `iOS/Inkwell/Rendering/MarkdownRendererView.swift` — removed `applyInlineFormatting()` and `byteRangeToAttrRange()` (never called, dead code)
- `iOS/InkwellTests/StandardSiteTests.swift` — updated `publicationVerificationURL()` test to match new return type (`String?` → `String`)

---

## Tier 2 — Medium Impact, Medium Feasibility

### 4. Standard.site Post Embed Fetching

**iOS:** `iOS/Inkwell/Rendering/StandardSitePostEmbed.swift` (`loadDocument()`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/ui/reader/LeafletBlockRenderer.kt` (`fetchStandardSitePost()`)

**What's duplicated:**
- AT-URI parsing to extract DID/collection/rkey
- Document record fetching from PDS
- Publication matching logic: `doc.record.site == pub.uri || pub.record.url == doc.record.site`
- Cover image extraction and CDN URL building

**Effort:** Medium (~150 lines of business logic, but embedded in platform-specific view code)  
**Risk:** Medium — requires extracting pure logic from view-layer code  
**Benefit:** Ensures both platforms resolve embedded Standard.site posts identically

---

### 5. AT-URI Type Consolidation (iOS)

**iOS:** `iOS/Inkwell/Protocols/StandardSite/StandardSiteTypes.swift` (native `ATURI` struct)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/data/model/common/Models.kt` (`typealias AtUri = uk.ewancroft.inkwell.shared.AtUri`)

**What's duplicated:**
- iOS has its own `ATURI` struct with `parse()` method, used in 21+ call sites
- Android already uses the shared `AtUri` type directly
- iOS `ATURI` and shared `AtUri` have identical semantics

**Effort:** Medium (21 call sites to update, bridging code needed)  
**Risk:** Medium — pervasive type used throughout iOS codebase  
**Benefit:** Single AT-URI implementation; bug fixes apply to both platforms automatically

---

### 6. Notification Polling Document-Matching Logic

**iOS:** `iOS/Inkwell/Features/Subscriptions/NotificationManager.swift` (`pollForNewDocuments()`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/data/remote/InkwellNotificationManager.kt` (`pollForNewDocuments()`)

**What's duplicated:**
- Publication-matching condition: `site == sub.publicationUri || pubUrl != null && (site == pubUrl || site.startsWith("$pubUrl/"))`
- New-document detection via `allSeenURIs.contains(uri)`
- Sorting newest-first by `publishedAt`
- Document-to-publication association rules

**Effort:** Medium (~100 lines of pure logic embedded in platform-specific polling)  
**Risk:** Medium — notification polling is sensitive; requires careful testing  
**Benefit:** Both platforms notify about the same documents in the same way

---

### 7. Content Type Detection / Provider Selection

**iOS:** `iOS/Inkwell/Rendering/ContentProvider.swift` (`ProviderRegistry.detectProvider()`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/ui/reader/PostDetailViewModel.kt` (`parseContent()`)

**What's duplicated:**
- Format-type string dispatch: `"pub.leaflet.content"` → Leaflet, `"at.markpub.markdown"` → Markpub, `"blog.pckt.content"` → Pckt, `"app.offprint.content"` → Offprint
- Fallback to `textContent` plaintext
- Blob-pages detection and handling

**Effort:** Low-Medium (~50 lines of dispatch logic)  
**Risk:** Low — pure string-matching dispatch  
**Benefit:** Both platforms detect document content format identically

---

### 8. Record Entry Parsing (rkey Extraction)

**iOS:** `iOS/Inkwell/Authentication/LoginStateManager.swift` (`fetchSubscriptions`, `fetchRecommends`, `fetchComments`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/data/repository/PdsRepository.kt` (same methods)

**What's duplicated:**
- AT-URI parsing to extract rkey: `AtUri.parse(entry.uri)?.recordKey`
- Record entry construction from raw XRPC response
- Pagination with max-records cap and cursor-stuck detection

**Effort:** Medium (~200 lines across multiple methods)  
**Risk:** Medium — tightly coupled to networking I/O in both platforms  
**Benefit:** Reduces duplication in repository/record-fetching layer

---

## Tier 3 — Low Impact or Hard to Migrate

### 9. Theme Resolution (iOS Native Wrapper)

**iOS:** `iOS/Inkwell/Rendering/ReaderTheme.swift`  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/ui/reader/ReaderTheme.kt`

**Status:** Already partially shared. Android delegates to `SharedReaderTheme.resolve()`. iOS still has its own `ReaderTheme` struct with SwiftUI-specific `Color`/`Font` types, but the pure cascade logic is already in shared KMP.

**Effort:** Low (adapter consolidation only)  
**Risk:** Low  
**Benefit:** Cleaner architecture, but low functional impact

---

### 10. Search/Discovery Computed Properties

**iOS:** `iOS/Inkwell/Features/Discover/StandardReaderAPI.swift` (`ReaderSearchResult.isPublication`, `isStandardSiteDocument`, `webURL`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/data/model/common/SearchModels.kt` (`SearchResult.isPublication`, `isStandardSiteDocument`)

**What's duplicated:**
- `isPublication`: checks `type == "site.standard.publication"`
- `isStandardSiteDocument`: checks `type == "site.standard.document"`
- `webURL`: constructs URL from `basePath` + `path` + `rkey`

**Effort:** Low (~30 lines)  
**Risk:** Low  
**Benefit:** Shared search result classification

---

### 11. Bluesky Post Fetching & Caching Pattern

**iOS:** `iOS/Inkwell/Rendering/BSkyPostEmbed.swift` (`BSkyPostFetcher`, `BSkyPostCache`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/data/remote/BSkyPostFetcher.kt`

**Status:** Partially duplicated, but heavily coupled to platform networking. The cache-deduplication pattern is similar but not identical.

**Effort:** High (requires abstracting networking layer)  
**Risk:** High — networking I/O, caching semantics differ  
**Benefit:** Moderate — reduces duplication but requires careful cache design

---

### 12. Bluesky Profile Fetching & Caching Pattern

**iOS:** `iOS/Inkwell/Rendering/BSkyProfileFetcher.swift` (`BSkyProfileFetcher`, `BSkyProfileCache`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/data/repository/PdsRepository.kt` (`getProfile()`, `resolveHandle()`)

**Status:** Similar patterns but different architectures. Handle normalization (`lowercased`, removing `@`) is pure logic.

**Effort:** Medium  
**Risk:** Medium  
**Benefit:** Moderate — handle normalization could be shared

---

## Not Migratable (Platform-Specific by Design)

| Category | iOS | Android | Reason |
|----------|-----|---------|--------|
| Authentication/Security | `LoginStateManager.swift`, `KeychainStore.swift`, `DPoPJWTGenerator.swift` | `AndroidOAuthSessionStore.kt` | Keychain vs EncryptedSharedPreferences; CryptoKit P-256 |
| Background Task Scheduling | `BackgroundRefreshManager.swift` (BGTaskScheduler) | `InkwellNotificationWorker.kt` (WorkManager) | OS-specific frameworks |
| Networking I/O | URLSession-based code | OkHttp-based code | Platform HTTP libraries |
| Platform Serialization | ATProtoKit `ATRecordProtocol` | kotlinx.serialization `@Serializable` | Different serialization frameworks |
| Color Type Conversion | SwiftUI `Color` | Compose `Color` | Platform graphics frameworks |
| Date Formatting | Foundation `DateFormatter` | `DateTimeFormatter` | Platform date APIs |
| App Version/Context | N/A | `AppVersion.kt` (PackageManager) | Android-only |

---

## Recommended Execution Order

1. **`formatCount` utility** ✅ COMPLETED
2. **XRPC endpoints & OAuth scopes** ✅ COMPLETED — all iOS hardcoded `/xrpc/...` paths and scope strings now use shared KMP wrappers; public API URLs (Bluesky, Constellation) also migrated
3. **Search result classification** ✅ COMPLETED — iOS `ReaderSearchResult` computed properties now delegate to shared `SearchResultClassifier`
4. **Publication matching** ✅ COMPLETED — new shared `PublicationMatcher.documentBelongsToPublication()` used by both platforms' notification and document-browsing code
5. **Collection NSIDs** ✅ COMPLETED — poll definition/vote and comment collection strings migrated to shared `CollectionNsids`
6. **Dead code removal** ✅ COMPLETED — removed unused iOS `applyInlineFormatting()` scanner and `byteRangeToAttrRange()`
7. **Content format types** ✅ COMPLETED — shared converters for Leaflet/pckt/Offprint/Markpub with `ContentFormatDispatcher`, `BlockLossLabels`, and `JsonMapBridge`; Android `MarkdownConverter` and `PcktOffprintConverter` reduced to thin adapters
8. **Standard.site post embed fetching** — publication matching shared; document fetching remains platform-specific due to different networking stacks
9. **AT-URI type consolidation** — medium effort, pervasive but straightforward
10. **Content type detection** — low effort, low risk
11. **Record entry parsing** — higher effort, tightly coupled to I/O
12. **Bluesky post/profile fetching patterns** — high effort, networking-coupled
