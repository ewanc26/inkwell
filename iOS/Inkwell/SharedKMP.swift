//
//  SharedKMP.swift
//  Inkwell
//
//  Swift wrappers around the Kotlin Multiplatform shared core.
//  The InkwellShared.xcframework is built from shared/ and
//  contains pure business logic shared between platforms.
//

import Foundation
import InkwellShared

// MARK: - AT-URI Parser

func parseAtUri(_ uri: String) -> (did: String, collection: String, recordKey: String)? {
    let kotlinUri = AtUri.companion.parse(uri: uri)
    guard let kotlinUri else { return nil }
    return (did: kotlinUri.did, collection: kotlinUri.collection, recordKey: kotlinUri.recordKey)
}

// MARK: - Facet Schema

struct FacetSchema {
    let facet: String
    let byteSlice: String
    let bold: String
    let italic: String
    let code: String
    let strike: String
    let link: String
    let lossy: [String: String]

    static let leaflet: FacetSchema = {
        let d = InkwellShared.FacetSchema.shared.leaflet
        return FacetSchema(
            facet: d.facet as String,
            byteSlice: d.byteSlice as String,
            bold: d.bold as String,
            italic: d.italic as String,
            code: d.code as String,
            strike: d.strike as String,
            link: d.link as String,
            lossy: d.lossy as? [String: String] ?? [:]
        )
    }()
    static let pckt: FacetSchema = {
        let d = InkwellShared.FacetSchema.shared.pckt
        return FacetSchema(
            facet: d.facet as String,
            byteSlice: d.byteSlice as String,
            bold: d.bold as String,
            italic: d.italic as String,
            code: d.code as String,
            strike: d.strike as String,
            link: d.link as String,
            lossy: d.lossy as? [String: String] ?? [:]
        )
    }()
    static let offprint: FacetSchema = {
        let d = InkwellShared.FacetSchema.shared.offprint
        return FacetSchema(
            facet: d.facet as String,
            byteSlice: d.byteSlice as String,
            bold: d.bold as String,
            italic: d.italic as String,
            code: d.code as String,
            strike: d.strike as String,
            link: d.link as String,
            lossy: d.lossy as? [String: String] ?? [:]
        )
    }()
}

// MARK: - Facet Converter

func facetsToMarkdown(_ plaintext: String, facets: [LeafletFacet]?, schema: FacetSchema) -> String {
    var lost = Set<String>()
    return facetsToMarkdown(plaintext, facets: facets, schema: schema, lost: &lost)
}

func facetsToMarkdown(_ plaintext: String, facets: [LeafletFacet]?, schema: FacetSchema, lost: inout Set<String>) -> String {
    let sharedFacets = facets?.map { facetToShared($0) }
    let result = InkwellShared.FacetConverter.shared.facetsToMarkdown(
        plaintext: plaintext,
        facets: sharedFacets,
        boldType: schema.bold,
        italicType: schema.italic,
        codeType: schema.code,
        strikeType: schema.strike,
        linkType: schema.link,
        lossy: schema.lossy,
        lost: nil
    )
    return result
}

func markdownToFacets(_ markdown: String, schema: FacetSchema) -> (plaintext: String, facets: [LeafletFacet]) {
    let pair = InkwellShared.FacetConverter.shared.markdownToFacets(
        markdown: markdown,
        boldType: schema.bold,
        italicType: schema.italic,
        codeType: schema.code,
        strikeType: schema.strike,
        linkType: schema.link
    )
    let sharedFacets = pair.second as? [RichTextFacet] ?? []
    let plaintext = pair.first! as String
    return (plaintext: plaintext, facets: sharedFacets.map { sharedToFacet($0) })
}

// MARK: - Facet Type Bridging

private func facetToShared(_ facet: LeafletFacet) -> RichTextFacet {
    return RichTextFacet(
        byteStart: Int32(facet.index.byteStart),
        byteEnd: Int32(facet.index.byteEnd),
        features: facet.features.map { RichTextFeature(type: $0.type, uri: $0.uri) }
    )
}

private func sharedToFacet(_ facet: RichTextFacet) -> LeafletFacet {
    return LeafletFacet(
        index: LeafletByteSlice(byteStart: Int(facet.byteStart), byteEnd: Int(facet.byteEnd)),
        features: facet.features.map { LeafletFacetFeature(type: $0.type, uri: $0.uri) }
    )
}

// MARK: - Theme Resolution

func resolveReaderTheme(
    richBackgroundColor: Int? = nil,
    richPageBackgroundColor: Int? = nil,
    richPrimaryColor: Int? = nil,
    richAccentBackgroundColor: Int? = nil,
    richAccentTextColor: Int? = nil,
    richPageWidth: Int? = nil,
    richShowPageBackground: Bool? = nil,
    richHeadingFont: String? = nil,
    richBodyFont: String? = nil,
    richSharedFont: String? = nil,
    paletteBackground: String? = nil,
    paletteText: String? = nil,
    paletteLink: String? = nil,
    paletteAccent: String? = nil,
    paletteSurfaceHover: String? = nil,
    basicBackground: String? = nil,
    basicForeground: String? = nil,
    basicAccent: String? = nil,
    basicAccentForeground: String? = nil,
) -> SharedReaderTheme {
    SharedReaderTheme.Companion.shared.resolve(
        richBackgroundColor: richBackgroundColor.map { KotlinInt(value: Int32($0)) },
        richPageBackgroundColor: richPageBackgroundColor.map { KotlinInt(value: Int32($0)) },
        richPrimaryColor: richPrimaryColor.map { KotlinInt(value: Int32($0)) },
        richAccentBackgroundColor: richAccentBackgroundColor.map { KotlinInt(value: Int32($0)) },
        richAccentTextColor: richAccentTextColor.map { KotlinInt(value: Int32($0)) },
        richPageWidth: richPageWidth.map { KotlinInt(value: Int32($0)) },
        richShowPageBackground: richShowPageBackground.map { KotlinBoolean(value: $0) },
        richHeadingFont: richHeadingFont,
        richBodyFont: richBodyFont,
        richSharedFont: richSharedFont,
        paletteBackground: paletteBackground,
        paletteText: paletteText,
        paletteLink: paletteLink,
        paletteAccent: paletteAccent,
        paletteSurfaceHover: paletteSurfaceHover,
        basicBackground: basicBackground,
        basicForeground: basicForeground,
        basicAccent: basicAccent,
        basicAccentForeground: basicAccentForeground
    )
}

// MARK: - Markdown Parser / Serializer

func parseMarkdown(_ markdown: String) -> [MarkdownBlockNode] {
    let sharedBlocks = MarkdownParser.shared.parse(markdown: markdown)
    return sharedBlocks.map { sharedBlockToLocal($0) }
}

func serializeMarkdown(_ blocks: [MarkdownBlockNode]) -> String {
    let sharedBlocks = blocks.map { localBlockToShared($0) }
    return MarkdownSerializer.shared.serialize(blocks: sharedBlocks)
}

// MARK: - Markdown Type Bridging

private func sharedBlockToLocal(_ block: MarkdownBlock) -> MarkdownBlockNode {
    switch block {
    case let heading as MarkdownBlock.Heading:
        return .heading(level: Int(heading.level), text: heading.text as String)
    case let paragraph as MarkdownBlock.Paragraph:
        return .paragraph(text: paragraph.text as String)
    case let code as MarkdownBlock.Code:
        return .code(language: code.language as String?, content: code.content as String)
    case let math as MarkdownBlock.Math:
        return .math(tex: math.tex as String)
    case let blockquote as MarkdownBlock.Blockquote:
        return .blockquote(text: blockquote.text as String)
    case let image as MarkdownBlock.Image:
        return .image(alt: image.alt as String, url: image.url as String)
    case _ as MarkdownBlock.HorizontalRule:
        return .horizontalRule
    case let list as MarkdownBlock.UnorderedList:
        return .unorderedList(items: list.items.map { sharedListItemToLocal($0) })
    case let list as MarkdownBlock.OrderedList:
        return .orderedList(start: Int(list.start), items: list.items.map { sharedListItemToLocal($0) })
    case let list as MarkdownBlock.TaskList:
        return .taskList(items: list.items.map { sharedListItemToLocal($0) })
    default:
        return .paragraph(text: "")
    }
}

private func localBlockToShared(_ block: MarkdownBlockNode) -> MarkdownBlock {
    switch block {
    case .heading(let level, let text):
        return MarkdownBlock.Heading(level: Int32(level), text: text)
    case .paragraph(let text):
        return MarkdownBlock.Paragraph(text: text)
    case .code(let language, let content):
        return MarkdownBlock.Code(language: language, content: content)
    case .math(let tex):
        return MarkdownBlock.Math(tex: tex)
    case .blockquote(let text):
        return MarkdownBlock.Blockquote(text: text)
    case .image(let alt, let url):
        return MarkdownBlock.Image(alt: alt, url: url)
    case .horizontalRule:
        return MarkdownBlock.HorizontalRule.shared
    case .unorderedList(let items):
        return MarkdownBlock.UnorderedList(items: items.map { localListItemToShared($0) })
    case .orderedList(let start, let items):
        return MarkdownBlock.OrderedList(start: Int32(start), items: items.map { localListItemToShared($0) })
    case .taskList(let items):
        return MarkdownBlock.TaskList(items: items.map { localListItemToShared($0) })
    }
}

private func sharedListItemToLocal(_ item: MarkdownListItem) -> MarkdownListItemNode {
    return MarkdownListItemNode(
        text: item.text as String,
        checked: item.checked?.boolValue,
        children: item.children?.map { sharedListItemToLocal($0) }
    )
}

private func localListItemToShared(_ item: MarkdownListItemNode) -> MarkdownListItem {
    let sharedChecked: KotlinBoolean? = item.checked.map { KotlinBoolean(value: $0) }
    return MarkdownListItem(
        text: item.text as String,
        checked: sharedChecked,
        children: item.children?.map { localListItemToShared($0) }
    )
}

// MARK: - Tip Prompt Policy

func shouldShowTip(
    launchCount: Int,
    lastShownEpochMillis: Int64 = -1,
    nowEpochMillis: Int64
) -> Bool {
    TipPromptPolicy.shared.shouldShowTip(
        launchCount: Int32(launchCount),
        lastShownEpochMillis: lastShownEpochMillis,
        nowEpochMillis: nowEpochMillis
    )
}

// MARK: - Notification Policy

enum NotificationStyle {
    case none
    case single
    case summary(count: Int32)
}

func notificationStyle(newDocCount: Int32) -> NotificationStyle {
    let style = NotificationPolicy.shared.notificationStyle(newDocCount: newDocCount)
    if style is NotificationStyleNone {
        return .none
    } else if style is NotificationStyleSingle {
        return .single
    } else if let summary = style as? NotificationStyleSummary {
        return .summary(count: Int32(clamping: summary.count))
    }
    return .none
}

func isFirstPoll(lastPollEpochMillis: Int64 = -1) -> Bool {
    NotificationPolicy.shared.isFirstPoll(lastPollEpochMillis: lastPollEpochMillis)
}

func trimSeenUris(_ uris: [String]) -> [String] {
    NotificationPolicy.shared.trimSeenUris(seenUris: uris)
}

func trimNotifications(_ notifications: [Any]) -> [Any] {
    NotificationPolicy.shared.trimNotifications(notifications: notifications)
}

// MARK: - Verification URLs

func sharedPublicationVerificationURL(for publicationURL: String) -> String? {
    VerificationUrls.shared.publicationVerificationUrl(publicationUrl: publicationURL)
}

func sharedDocumentCanonicalURL(documentSite: String, documentPath: String?, publicationURL: String?) -> String? {
    VerificationUrls.shared.documentCanonicalUrl(documentSite: documentSite, documentPath: documentPath, publicationUrl: publicationURL)
}

func sharedDiscoveryLinkTag(forRecordURI recordURI: String, relation: String) -> String {
    VerificationUrls.shared.discoveryLinkTag(recordURI: recordURI, relation: relation)
}

func sharedContainsDocumentLink(html: String, documentURI: String) -> Bool {
    DocumentLinkScanner.shared.containsDocumentLink(html: html, documentURI: documentURI)
}

// MARK: - Constellation Pagination

struct SharedBacklink {
    let did: String
    let collection: String
    let rkey: String
    var recordURI: String { "at://\(did)/\(collection)/\(rkey)" }
}

func deduplicateBacklinks(_ backlinks: [SharedBacklink]) -> [SharedBacklink] {
    let shared = backlinks.map { InkwellShared.ConstellationBacklink(did: $0.did, collection: $0.collection, rkey: $0.rkey) }
    let deduped = ConstellationPagination.shared.deduplicate(backlinks: shared)
    return deduped.map { SharedBacklink(did: $0.did, collection: $0.collection, rkey: $0.rkey) }
}

// MARK: - URL Utilities

func normalizedSite(_ value: String) -> String {
    UrlUtils.shared.normalizedSite(value: value)
}

func canonicalUrl(site: String, path: String?, publicationURL: String? = nil) -> String? {
    UrlUtils.shared.canonicalUrl(site: site, path: path, publicationUrl: publicationURL)
}

// MARK: - Number Formatting

func sharedFormatCount(_ count: Int) -> String {
    NumberFormat.shared.formatCount(count: Int32(count))
}

// MARK: - XRPC Endpoints

func sharedXrpcRepoCreateRecord() -> String { XrpcEndpoints.shared.REPO_CREATE_RECORD }
func sharedXrpcRepoDeleteRecord() -> String { XrpcEndpoints.shared.REPO_DELETE_RECORD }
func sharedXrpcRepoGetRecord() -> String { XrpcEndpoints.shared.REPO_GET_RECORD }
func sharedXrpcRepoListRecords() -> String { XrpcEndpoints.shared.REPO_LIST_RECORDS }
func sharedXrpcSyncGetBlob() -> String { XrpcEndpoints.shared.SYNC_GET_BLOB }
func sharedXrpcServerGetSession() -> String { XrpcEndpoints.shared.SERVER_GET_SESSION }
func sharedXrpcIdentityResolveHandle() -> String { XrpcEndpoints.shared.IDENTITY_RESOLVE_HANDLE }
func sharedXrpcActorGetProfile() -> String { XrpcEndpoints.shared.ACTOR_GET_PROFILE }
func sharedXrpcFeedGetPosts() -> String { XrpcEndpoints.shared.FEED_GET_POSTS }
func sharedXrpcMicrocosmGetBacklinks() -> String { XrpcEndpoints.shared.MICROCOSM_GET_BACKLINKS }
func sharedPublicBskyApi() -> String { XrpcEndpoints.shared.PUBLIC_BSKY_API }
func sharedConstellationApi() -> String { XrpcEndpoints.shared.CONSTELLATION_API }

// MARK: - OAuth Scopes

func sharedOAuthScopeAtproto() -> String { OAuthScopes.shared.ATPROTO }
func sharedOAuthScopeBlobAll() -> String { OAuthScopes.shared.BLOB_ALL }
func sharedOAuthScopeRepoPublication() -> String { OAuthScopes.shared.REPO_PUBLICATION }
func sharedOAuthScopeRepoDocument() -> String { OAuthScopes.shared.REPO_DOCUMENT }
func sharedOAuthScopeRepoSubscription() -> String { OAuthScopes.shared.REPO_SUBSCRIPTION }
func sharedOAuthScopeRepoRecommend() -> String { OAuthScopes.shared.REPO_RECOMMEND }
func sharedOAuthScopeAuthFull() -> String { OAuthScopes.shared.AUTH_FULL }
func sharedOAuthScopeAuthSocial() -> String { OAuthScopes.shared.AUTH_SOCIAL }

// MARK: - Content Format Detection

func sharedContentFormat(type: String?) -> SharedContentFormat? {
    guard let type else { return nil }
    if ContentFormatDetector.shared.isKnown(type: type) {
        if type == ContentFormatDetector.shared.LEAFLET { return .leaflet }
        if type == ContentFormatDetector.shared.MARKPUB { return .markpub }
        if type == ContentFormatDetector.shared.PCKT { return .pckt }
        if type == ContentFormatDetector.shared.OFFPRINT { return .offprint }
    }
    return nil
}

enum SharedContentFormat: String {
    case leaflet
    case markpub
    case pckt
    case offprint

    var typeString: String {
        switch self {
        case .leaflet: return ContentFormatDetector.shared.LEAFLET
        case .markpub: return ContentFormatDetector.shared.MARKPUB
        case .pckt: return ContentFormatDetector.shared.PCKT
        case .offprint: return ContentFormatDetector.shared.OFFPRINT
        }
    }
}

func sharedIsPcktOrOffprint(type: String?) -> Bool {
    ContentFormatDetector.shared.isPcktOrOffprint(type: type)
}

// MARK: - Leaflet Block Types

func sharedLeafletBlockType(_ type: String) -> String? {
    let t = LeafletTypes.shared
    switch type {
    case t.BLOCKS_TEXT: return t.BLOCKS_TEXT
    case t.BLOCKS_HEADER: return t.BLOCKS_HEADER
    case t.BLOCKS_PARAGRAPH: return t.BLOCKS_PARAGRAPH
    case t.BLOCKS_BLOCKQUOTE: return t.BLOCKS_BLOCKQUOTE
    case t.BLOCKS_CODE: return t.BLOCKS_CODE
    case t.BLOCKS_MATH: return t.BLOCKS_MATH
    case t.BLOCKS_IMAGE: return t.BLOCKS_IMAGE
    case t.BLOCKS_HORIZONTAL_RULE: return t.BLOCKS_HORIZONTAL_RULE
    case t.BLOCKS_UNORDERED_LIST: return t.BLOCKS_UNORDERED_LIST
    case t.BLOCKS_ORDERED_LIST: return t.BLOCKS_ORDERED_LIST
    case t.BLOCKS_CHECKLIST: return t.BLOCKS_CHECKLIST
    case t.BLOCKS_BSKY_POST: return t.BLOCKS_BSKY_POST
    case t.BLOCKS_STANDARD_SITE_POST: return t.BLOCKS_STANDARD_SITE_POST
    case t.BLOCKS_WEBSITE: return t.BLOCKS_WEBSITE
    case t.BLOCKS_IFRAME: return t.BLOCKS_IFRAME
    case t.BLOCKS_BUTTON: return t.BLOCKS_BUTTON
    case t.BLOCKS_DIVIDER: return t.BLOCKS_DIVIDER
    case t.BLOCKS_PAGE: return t.BLOCKS_PAGE
    case t.BLOCKS_POSTS_LIST: return t.BLOCKS_POSTS_LIST
    case t.BLOCKS_SIGNUP: return t.BLOCKS_SIGNUP
    case t.BLOCKS_POLL: return t.BLOCKS_POLL
    default: return nil
    }
}

// MARK: - Bluesky Embed Types

func sharedBlueskyEmbedImages() -> String { BlueskyEmbedTypes.shared.IMAGES }
func sharedBlueskyEmbedExternal() -> String { BlueskyEmbedTypes.shared.EXTERNAL }
func sharedBlueskyEmbedRecord() -> String { BlueskyEmbedTypes.shared.RECORD }

// MARK: - CDN URLs

func sharedBskyThumbnail(did: String, link: String) -> String {
    CdnUrls.shared.bskyThumbnail(did: did, link: link)
}

// MARK: - Collection NSIDs

func sharedCollectionPublication() -> String { CollectionNsids.shared.PUBLICATION }
func sharedCollectionDocument() -> String { CollectionNsids.shared.DOCUMENT }
func sharedCollectionSubscription() -> String { CollectionNsids.shared.GRAPH_SUBSCRIPTION }
func sharedCollectionRecommend() -> String { CollectionNsids.shared.GRAPH_RECOMMEND }
func sharedLeafletComment() -> String { CollectionNsids.shared.LEAFLET_COMMENT }
func sharedLeafletPollDefinition() -> String { CollectionNsids.shared.LEAFLET_POLL_DEFINITION }
func sharedLeafletPollVote() -> String { CollectionNsids.shared.LEAFLET_POLL_VOTE }

// MARK: - Search Backend

func sharedSearchBackendUrl() -> String { SearchBackendUrl.shared.BASE }

// MARK: - Publication Matching

func sharedDocumentBelongsToPublication(documentSite: String, publicationUri: String, publicationUrl: String?) -> Bool {
    PublicationMatcher.shared.documentBelongsToPublication(
        documentSite: documentSite,
        publicationUri: publicationUri,
        publicationUrl: publicationUrl
    )
}

// MARK: - Search Result Classification

func sharedIsPublication(type: String) -> Bool {
    SearchResultClassifier.shared.isPublication(type: type)
}

func sharedIsStandardSiteDocument(uri: String) -> Bool {
    SearchResultClassifier.shared.isStandardSiteDocument(uri: uri)
}

func sharedWebURL(basePath: String?, path: String?, rkey: String?, platform: String?, isPublication: Bool) -> String? {
    SearchResultClassifier.shared.webURL(
        basePath: basePath,
        path: path,
        rkey: rkey,
        platform: platform,
        isPublication: isPublication
    )
}

// MARK: - Handle Utilities

func sharedNormalizeHandle(_ handle: String) -> String {
    HandleUtils.shared.normalize(handle: handle)
}

// MARK: - String Utilities

func sharedTrimTrailingSlash(_ value: String) -> String {
    StringUtils.shared.trimTrailingSlash(value: value)
}

// MARK: - UTF-8 Offsets

func sharedByteRangeToCharRange(_ text: String, byteStart: Int, byteEnd: Int) -> Range<Int>? {
    let result = Utf8Offsets.shared.byteRangeToCharRange(text: text, byteStart: Int32(byteStart), byteEnd: Int32(byteEnd))
    guard let result else { return nil }
    return Int(result.first)..<Int(result.last) + 1
}

func sharedCharIndexToByteOffset(_ text: String, charIndex: Int) -> Int {
    Int(Utf8Offsets.shared.charIndexToByteOffset(text: text, charIndex: Int32(charIndex)))
}

func sharedByteLength(_ text: String) -> Int {
    Int(Utf8Offsets.shared.byteLength(text: text))
}

// MARK: - Content Format Converters

/// Shared conversion result matching the KMP `SharedConvertResult`.
struct SharedConvertResult {
    let markdown: String
    let lost: [String]
}

/// Shared write result matching the KMP `SharedWriteResult`.
struct SharedWriteResult {
    let content: [String: Any]
    let lost: [String]
}

/// Converts a content dictionary (with `$type` key) to markdown blocks + lost labels.
func sharedContentToMarkdown(_ content: [String: Any], authorDid: String = "") -> SharedConvertResult {
    let converter = ContentFormatDispatcher.shared
    let result = converter.toMarkdown(content: content, authorDid: authorDid)
    let lostArray = (result.lost as? Set<String>) ?? []
    let mdString = MarkdownSerializer.shared.serialize(blocks: result.blocks)
    return SharedConvertResult(markdown: mdString, lost: Array(lostArray))
}

/// Converts markdown to a format-specific content dictionary.
func sharedMarkdownToContent(_ markdown: String, format: String, uploadedBlobs: [String: [String: Any]] = [:]) -> SharedWriteResult {
    let converter = ContentFormatDispatcher.shared
    let result = converter.fromMarkdown(markdown: markdown, format: format, uploadedBlobs: uploadedBlobs)
    guard let dict = result.content as? [String: Any] else {
        return SharedWriteResult(content: [:], lost: [])
    }
    let lostArray = (result.lost as? Set<String>) ?? []
    return SharedWriteResult(content: dict, lost: Array(lostArray))
}

/// Returns loss labels for unsupported blocks in a given format.
func sharedBlockLossLabels(format: String) -> [String: String] {
    switch format {
    case "leaflet":
        return BlockLossLabels.shared.leaflet as? [String: String] ?? [:]
    case "pckt":
        return BlockLossLabels.shared.pckt as? [String: String] ?? [:]
    case "offprint":
        return BlockLossLabels.shared.offprint as? [String: String] ?? [:]
    default:
        return [:]
    }
}
