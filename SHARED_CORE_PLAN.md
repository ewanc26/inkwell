# Inkwell Shared Core — Kotlin Multiplatform Plan

## Goal
Extract duplicated business logic into a `shared/` KMP module so both platforms
call the same code. UI, networking I/O, and secure storage stay native.

## Architecture

```
inkwell/
├── shared/                          # NEW — KMP shared core
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── commonMain/
│   │   │   └── kotlin/uk/ewancroft/inkwell/shared/
│   │   │       ├── AtUri.kt
│   │   │       ├── markdown/
│   │   │       │   ├── MarkdownParser.kt
│   │   │       │   ├── MarkdownBlock.kt
│   │   │       │   └── MarkdownSerializer.kt
│   │   │       ├── facets/
│   │   │       │   ├── FacetSchema.kt
│   │   │       │   └── FacetConverter.kt
│   │   │       ├── verification/
│   │   │       │   ├── VerificationUrls.kt
│   │   │       │   └── DocumentLinkScanner.kt
│   │   │       ├── theme/
│   │   │       │   ├── ReaderTheme.kt
│   │   │       │   └── FontFamily.kt
│   │   │       ├── notification/
│   │   │       │   └── NotificationPolicy.kt
│   │   │       └── TipPromptPolicy.kt
│   │   ├── androidMain/             # Android-specific adapters (OkHttp wrappers, etc.)
│   │   └── iosMain/                  # iOS-specific adapters (URLSession wrappers, etc.)
│   │   └── commonTest/               # Shared unit tests
│   └── gradle.properties
├── iOS/                              # Unchanged structure, consumes shared.xcframework
└── Android/                          # Unchanged structure, depends on shared as AAR
```

## Module 1 — Pure-logic core (no I/O, no platform types)

These modules share 100% of their code with zero `expect/actual` needed.

### 1a. AT-URI Parser
- **Source**: `iOS/Inkwell/Protocols/StandardSite/StandardSiteTypes.swift` `ATURI` + `Android/.../data/model/common/Models.kt` `AtUri`
- **Target**: `shared/src/commonMain/.../AtUri.kt`
- **Logic**: Parse `at://did/collection/rkey`, reassemble URI. Identical semantics.

### 1b. Markdown Parser
- **Source**: `iOS/Inkwell/Rendering/ContentProvider.swift` `MarkdownParser` + `MarkdownBlock`/`MarkdownListItem` + `Android/.../reader/MarkdownParser.kt`
- **Target**: `shared/src/commonMain/.../markdown/`
- **Logic**: Verbatim port — headings, code blocks, HR, blockquotes, images, lists, task checkboxes, paragraphs. Near-identical line-by-line.

### 1c. Facet Schema Constants
- **Source**: `iOS/Inkwell/Rendering/ContentProvider.swift` `FacetSchema.leaflet/pckt/offprint` + `lossLabels` + `Android/.../writer/MarkdownConverter.kt` `facetPrefix` + `PcktOffprintConverter.kt` `pcktLossLabels`/`offprintLossLabels` + `Android/.../reader/LeafletBlockRenderer.kt` hard-coded NSIDs
- **Target**: `shared/src/commonMain/.../facets/FacetSchema.kt`
- **Logic**: Pure string constants — NSIDs for bold/italic/code/strike/link/byteSlice + per-provider lossy block-label maps.

### 1d. Reader Theme Resolution
- **Source**: `iOS/Inkwell/Rendering/ReaderTheme.swift` (`ReaderTheme.init`, `family(for:)`, `Color(hex:)`) + `Android/.../reader/ReaderTheme.kt` (`resolve(...)`, `resolveFontFamily`, `hexToColor`)
- **Target**: `shared/src/commonMain/.../theme/ReaderTheme.kt`
- **Logic**: Identical cascade: rich → legacy → basic → system defaults. Same font-family keyword matching (mono/lora/serif/atkinson/rounded→sans). Same 6-digit `#RRGGBB` hex parse.

### 1e. Tip-Prompt Gating
- **Source**: `iOS/Inkwell/Features/TipPromptManager.swift` + `Android/.../util/TipPromptManager.kt`
- **Target**: `shared/src/commonMain/.../TipPromptPolicy.kt`
- **Logic**: Identical thresholds (5 launches, 7-day cooldown).

### 1f. Notification Retention Policy
- **Source**: `iOS/Inkwell/Subscriptions/NotificationManager.swift` poll/compare + `Android/.../remote/InkwellNotificationManager.kt` poll/compare
- **Target**: `shared/src/commonMain/.../notification/NotificationPolicy.kt`
- **Logic**: 500-URI seen set, 50-notification cap, first-poll baseline, newest-first sort, single vs summary threshold.

## Module 2 — Logic + thin platform I/O adapters

These modules need `expect/actual` for the I/O boundary; the decision logic lives in `commonMain`.

### 2a. Verification URL Builders + Link Scanner
- **Source**: `iOS/.../SiteStandardVerification.swift` (URL construction + regex) + `Android/.../StandardSiteVerifier.kt` (same)
- **Target**: `shared/src/commonMain/.../verification/`
- **Share**: `.well-known` endpoint construction, document canonical URL, `<link>` tag regex, `discoveryLinkTag` string template.
- **Keep native**: HTTP fetch (URLSession vs OkHttp), caching (iOS none vs Android 5-min Mutex).

### 2b. Facet Byte-Range → Markdown Converter
- **Source**: `iOS/Inkwell/Rendering/ContentProvider.swift` `FacetConverter.facetsToMarkdown` + `Android/.../reader/PcktOffprintConverter.kt` `facetsToMarkdown`
- **Target**: `shared/src/commonMain/.../facets/FacetConverter.kt`
- **Logic**: Algorithmically identical — UTF-8 byte slices, boundary set, segment merge, markdown wrapping order. Input type differs (typed `LeafletFacet` vs raw `JsonArray`); bridge in platform `actual` layer.

### 2c. Constellation Pagination + Recommend-Count Optimization
- **Source**: `iOS/Inkwell/Rendering/ConstellationClient.swift` pagination helpers + `Android/.../remote/ConstellationClient.kt`
- **Target**: `shared/src/commonMain/.../constellation/Pagination.kt`
- **Share**: Pagination cursor logic, 50/page cap, recommend-count first-page-then-paginate optimization.

## Deferred (requires model decoupling first)

The typed DTOs (`PublicationRecord`, `DocumentRecord`, `LeafletBlock`, etc.) are
coupled to platform serialization frameworks (iOS `ATRecordProtocol`/`UnknownType`,
Android `@Serializable`). Sharing them requires a neutral shared-model layer
decoupled from both SDKs — a follow-on after the pure-logic modules are proven.

## Build Integration

### Android
- `Android/settings.gradle.kts`: `include(":shared").projectDir = rootDir.resolve("../shared")`
- `Android/app/build.gradle.kts`: `implementation(project(":shared"))`

### iOS
- Swift package at `SharedKMP_Package/` wraps the XCFramework with `SharedKMP.swift`
- XCFramework built at `shared/InkwellShared.xcframework`
- Add `SharedKMP_Package` as a local Swift package dependency in Xcode
- Swift imports `SharedKMP` and calls KMP APIs as native Swift types

## Execution Order

1. Create `shared/` module with Gradle KMP plugin + iOS targets + XCFramework export. ✅
2. Extract Module 1 (pure logic, no `expect/actual` needed). ✅
3. Write shared unit tests in `commonTest`. ✅
4. Wire Android to depend on `shared`; run `./gradlew assembleDebug`. ✅
5. Wire iOS to consume XCFramework; run `xcodebuild`. ✅
6. Extract Module 2 (verification URLs + facet converter + pagination) with `expect/actual` I/O. 🔄 FacetConverter done; verification + pagination pending.
7. Plan neutral shared-model layer for DTOs. 📋 Deferred.

## Completed Modules

### 1a. AT-URI Parser ✅
- Commits: `d547751`, `a1049eb`
- Shared: `shared/src/commonMain/kotlin/.../AtUri.kt`
- iOS bridge: `iOS/Inkwell/SharedKMP.swift` `parseAtUri()`
- Android: direct dependency on `shared`

### 1b. Markdown Parser ✅
- Commits: `eafd39d`, `d7e13d4`
- Shared: `shared/src/commonMain/kotlin/.../markdown/MarkdownParser.kt`, `MarkdownBlock.kt`, `MarkdownSerializer.kt`
- iOS bridge: `SharedKMP.swift` `parseMarkdown()`, `serializeMarkdown()`
- Android bridge: `Android/.../reader/MarkdownParser.kt`
- iOS `ContentProvider.swift` local types renamed to `MarkdownBlockNode`/`MarkdownListItemNode`, parser/serializer delegate to shared KMP

### 1c. Facet Schema Constants ✅
- Commits: `dad33b5`, `f075b9f`
- Shared: `shared/src/commonMain/kotlin/.../facets/FacetSchema.kt`
- Both platforms consume `FacetSchema.leaflet/pckt/offprint` + lossy maps

### 1d. Reader Theme Resolution ✅
- Commits: earlier shared core wiring
- Shared: `shared/src/commonMain/kotlin/.../theme/ReaderTheme.kt`
- iOS bridge: `SharedKMP.swift` `resolveReaderTheme()`
- Android: direct dependency on `shared`

### 1e. Tip-Prompt Gating ✅
- Commits: earlier shared core wiring
- Shared: `shared/src/commonMain/kotlin/.../TipPromptPolicy.kt`
- iOS bridge: `SharedKMP.swift` `shouldShowTip()`
- Android: direct dependency on `shared`

### 1f. Notification Retention Policy ✅
- Commits: earlier shared core wiring
- Shared: `shared/src/commonMain/kotlin/.../notification/NotificationPolicy.kt`
- iOS bridge: `SharedKMP.swift` `notificationStyle()`, `isFirstPoll()`, `trimSeenUris()`, `trimNotifications()`
- Android: direct dependency on `shared`

### 2b. Facet Byte-Range → Markdown Converter ✅
- Commits: `e87e385`
- Shared: `shared/src/commonMain/kotlin/.../facets/FacetConverter.kt` (`facetsToMarkdown` + `markdownToFacets`)
- iOS bridge: `SharedKMP.swift` `facetsToMarkdown()`, `markdownToFacets()` with `LeafletFacet` ↔ `RichTextFacet` conversion
- Android: `MarkdownConverter.kt` delegates `markdownToFacets` to shared, maps `RichTextFacet` → JSON facet objects
- XCFramework rebuilt with `@ObjCName` exports for `FacetConverter`, `RichTextFacet`, `RichTextFeature`

## Remaining Work

### 2a. Verification URL Builders + Link Scanner
- **Status**: Not started
- **Source**: iOS `SiteStandardVerification.swift` + Android `StandardSiteVerifier.kt`
- **Target**: `shared/src/commonMain/.../verification/`
- **Share**: `.well-known` endpoint construction, document canonical URL, `<link>` tag regex
- **Keep native**: HTTP fetch, caching

### 2c. Constellation Pagination + Recommend-Count Optimization
- **Status**: Not started
- **Source**: iOS `ConstellationClient.swift` + Android `ConstellationClient.kt`
- **Target**: `shared/src/commonMain/.../constellation/Pagination.kt`
- **Share**: Pagination cursor logic, 50/page cap, recommend-count optimization

## Risk Notes

- KMP adds ~2-4 MB to iOS binary (Kotlin/Native runtime).
- Kotlin/Native compile times are slower than pure Swift; use Gradle build cache + pre-built XCFramework.
- Debugging across Swift→Kotlin boundary is improving but not seamless; keep shared surface deliberate.
- SKIE plugin recommended for Swift-friendly `suspend`/`Flow` interop (add later if needed).
