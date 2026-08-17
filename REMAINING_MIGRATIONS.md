# Inkwell Shared Core — Remaining Migration Opportunities

_Last updated: 2026-08-17_

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

---

## Tier 1 — High Impact, High Feasibility

### 1. Content Format Conversion Block-Type Mapping

**iOS:** `iOS/Inkwell/Rendering/ContentProvider.swift`  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/ui/writer/MarkdownConverter.kt` + `Android/app/src/main/java/uk/ewancroft/inkwell/ui/reader/PcktOffprintConverter.kt`

**What's duplicated:**
- Block-type string mapping (`"pub.leaflet.blocks.header"` ↔ `.heading`, `"pub.leaflet.blocks.paragraph"` ↔ `.paragraph`, etc.)
- Loss-label dictionaries (which block types are unsupported per format)
- Image CID detection (`url.hasPrefix("baf") || url.hasPrefix("Qm")`)
- CDN URL construction (`https://cdn.bsky.app/img/feed_thumbnail/plain/{did}/{cid}`)
- Facet-to-markdown and markdown-to-facet format-specific `$type` string handling

**Effort:** High (~700 lines to unify, but algorithmically identical)  
**Risk:** Medium — requires careful mapping of platform-specific content format types to shared abstractions  
**Benefit:** Eliminates the largest remaining duplication; single source of truth for format conversion

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

### 3. Format Count Utility

**iOS:** `iOS/Inkwell/Rendering/BSkyPostEmbed.swift` (`formatCount(_ count: Int) -> String`)  
**Android:** `Android/app/src/main/java/uk/ewancroft/inkwell/ui/reader/LeafletBlockRenderer.kt` (`formatCount()`)

**What's duplicated:**
- Number abbreviation: `1,500,000` → `"1.5M"`, `2,300` → `"2.3K"`, etc.

**Effort:** Trivial (~5 lines)  
**Risk:** None  
**Benefit:** Eliminates trivial duplication; proves shared utility pattern

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

1. **`formatCount` utility** — trivial, low risk, proves the shared utility pattern
2. **Inline markdown rendering** — high impact, clearly duplicated, pure text processing
3. **Content format conversion** — highest impact but largest effort (~700 lines)
4. **Standard.site post embed fetching** — medium impact, clear duplication
5. **AT-URI type consolidation** — medium effort, pervasive but straightforward
6. **Notification document-matching** — medium effort, sensitive but testable
7. **Content type detection** — low effort, low risk
8. **Record entry parsing** — higher effort, tightly coupled to I/O
