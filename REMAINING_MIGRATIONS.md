# Inkwell Shared Core — Remaining Migration Opportunities

_Last updated: 2026-08-18 (revised after closing out remaining Tier 1/2 items)_

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

### 2. Inline Markdown Rendering (Byte-Range → Attributed String) ✅ COMPLETED

**Shared:** `shared/src/commonMain/kotlin/.../markdown/InlineMarkdownScanner.kt` (delimiter scanning), `shared/src/commonMain/kotlin/.../text/Utf8Offsets.kt` (byte-range ↔ char-range conversion)
**Android:** `MarkdownRendererView.kt` `renderInline()` delegates to `InlineMarkdownScanner`; `LeafletBlockRenderer.kt` `buildAnnotatedString()` delegates to `Utf8Offsets.byteRangeToCharRange`
**iOS:** never needed the scanner — facets are converted to markdown text first (`FacetConverter.facetsToMarkdown`, already shared) and rendered via native `AttributedString(markdown:)` parsing. `sharedByteRangeToCharRange()`/`sharedCharIndexToByteOffset()`/`sharedByteLength()` wrappers exist in `SharedKMP.swift` for any future direct byte-range use.

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

**Status:** Publication-matching logic is shared (`PublicationMatcher`, see 3d above). AT-URI parsing is shared. Remaining duplication is document record fetching + cover-image/CDN URL building, which is networking I/O — left platform-specific per the module boundary (pure logic vs. I/O) this plan draws everywhere else.

---

### 5. AT-URI Type Consolidation (iOS) — reviewed, not pursuing

**iOS:** `iOS/Inkwell/Protocols/StandardSite/StandardSiteTypes.swift` (native `ATURI` struct)

**Finding:** `ATURI.parse()` already delegates 100% to the shared `parseAtUri()` (see 1a) — there is no remaining *logic* duplication, only a thin Swift struct wrapper for idiomatic `Equatable`/`Hashable` use in SwiftUI. Replacing the struct with the KMP class directly across 21+ call sites would be a pure type swap with no behavioral change — real refactor risk for no reduction in duplicated logic. Not pursuing.

---

### 6. Notification Polling Document-Matching Logic ✅ COMPLETED

**Shared:** `shared/src/commonMain/kotlin/.../content/PublicationMatcher.kt` (document-belongs-to-publication matching, see 3d), `shared/src/commonMain/kotlin/.../policy/NotificationPolicy.kt` (seen-URI cap, notification cap, first-poll baseline, single-vs-summary threshold)
**iOS/Android:** both `pollForNewDocuments()` implementations use the same publication-matching call and the same retention/threshold constants; sort-newest-first and seen-set dedup are trivial one-liners left inline on each platform (not worth extracting further).

---

### 7. Content Type Detection / Provider Selection ✅ COMPLETED

**Shared:** `shared/src/commonMain/kotlin/.../content/ContentFormatDetector.kt`
**Android:** `PostDetailViewModel.kt`, `WriterViewModel.kt`, `PdsRepository.kt` all use `ContentFormatDetector`
**iOS:** `SharedKMP.swift` `sharedContentFormat(type:)` wrapper used by `ContentProvider.swift`

---

### 8. Record Entry Parsing (rkey Extraction) — substantially complete

**iOS:** `iOS/Inkwell/Authentication/LoginStateManager.swift` (`fetchSubscriptions`, `fetchRecommends`, `fetchComments`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/data/repository/PdsRepository.kt` (same methods)

**Status:** rkey extraction already delegates to the shared AT-URI parser on both platforms. While auditing this item, found the two platforms' pagination safety-caps had drifted (iOS 1,000 records vs. Android 500 for the equivalent `listAllRecords` call) — unified via new `shared/src/commonMain/kotlin/.../policy/RecordListPolicy.kt` (`MAX_RECORDS = 500`), wired into both. The remaining duplication (raw XRPC response → record-entry construction, cursor loop) is networking I/O boilerplate, left platform-specific per the same pure-logic-vs-I/O boundary as everywhere else in this plan.

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

### 10. Search/Discovery Computed Properties ✅ COMPLETED

Duplicate of item 3c above — see there. Shared `SearchResultClassifier` covers `isPublication`, `isStandardSiteDocument`, and `webURL` construction for both platforms.

---

### 11. Bluesky Post Fetching & Caching Pattern — reviewed, not pursuing

**iOS:** `iOS/Inkwell/Rendering/BSkyPostEmbed.swift` (`BSkyPostFetcher`, `BSkyPostCache`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/data/remote/BSkyPostFetcher.kt`

**Finding:** Checked for extractable pure logic (cache TTLs, dedup-key formats) similar to what turned up in item 8 — found none; iOS's cache is a plain in-memory dedup dict with no shared constants to unify. Genuine duplication here is the fetch/cache orchestration itself, which is coupled to each platform's networking stack (URLSession vs OkHttp) and would need a real abstraction layer to share. Matches the plan's original high-effort/high-risk assessment — not pursuing without a specific need driving it.

---

### 12. Bluesky Profile Fetching & Caching Pattern — handle normalization done, rest not pursued

**iOS:** `iOS/Inkwell/Rendering/BSkyProfileFetcher.swift` (`BSkyProfileFetcher`, `BSkyProfileCache`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/data/repository/PdsRepository.kt` (`getProfile()`, `resolveHandle()`)

**Status:** Handle normalization (lowercasing, stripping `@`) is now shared via `shared/src/commonMain/kotlin/.../util/HandleUtils.kt` (`sharedNormalizeHandle()` on iOS, direct use on Android). The remaining fetch/cache orchestration has the same networking-coupling issue as item 11 — not pursuing.

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
8. **Standard.site post embed fetching** ✅ publication matching shared; document fetching remains platform-specific due to different networking stacks
9. **AT-URI type consolidation** — reviewed and **not pursued**: `ATURI` already delegates 100% to shared logic, so this would be a 21-site type swap with no reduction in duplicated logic
10. **Content type detection** ✅ COMPLETED — shared `ContentFormatDetector`
11. **Notification polling** ✅ COMPLETED — shared `PublicationMatcher` + `NotificationPolicy`
12. **Record entry parsing** ✅ substantially complete — rkey extraction already shared; found and fixed a real iOS/Android pagination-cap mismatch (1,000 vs 500) via new shared `RecordListPolicy`
13. **Bluesky post/profile fetching patterns** — reviewed: handle normalization now shared (`HandleUtils`); the fetch/cache orchestration itself stays platform-specific (networking-coupled, no extractable pure logic found)

### Status

All Tier 1 items and the feasible Tier 2/3 items are done. What's left (full AT-URI type consolidation, Bluesky fetch/cache abstraction) was evaluated and intentionally not pursued — either no logic duplication remains to extract, or the remaining duplication is networking orchestration that would need a real abstraction layer to share safely, which the risk didn't justify without a concrete driver.
