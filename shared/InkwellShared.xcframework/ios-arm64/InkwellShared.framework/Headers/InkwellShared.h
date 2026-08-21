#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class FacetConverter, InkwellSharedAtUri, InkwellSharedAtUriCompanion, InkwellSharedBasicTheme, InkwellSharedBlobRef, InkwellSharedBlockLossLabels, InkwellSharedBlueskyEmbedTypes, InkwellSharedByteSlice, InkwellSharedCdnUrls, InkwellSharedCollectionNsids, InkwellSharedColorValue, InkwellSharedConstellationBacklink, InkwellSharedConstellationPagination, InkwellSharedConstellationResponse, InkwellSharedConstellationSourcePaths, InkwellSharedContentFormatDetector, InkwellSharedContentFormatDispatcher, InkwellSharedDocumentLinkScanner, InkwellSharedDocumentPreferences, InkwellSharedFacetDefinition, InkwellSharedFacetSchema, InkwellSharedHandleUtils, InkwellSharedInlineMarkdownScanner, InkwellSharedInlineSegment, InkwellSharedInlineSegmentBold, InkwellSharedInlineSegmentCode, InkwellSharedInlineSegmentItalic, InkwellSharedInlineSegmentLink, InkwellSharedInlineSegmentPlain, InkwellSharedInlineSegmentStrike, InkwellSharedJsonMapBridge, InkwellSharedKotlinArray<T>, InkwellSharedKotlinEnum<E>, InkwellSharedKotlinEnumCompanion, InkwellSharedKotlinException, InkwellSharedKotlinIllegalStateException, InkwellSharedKotlinIntIterator, InkwellSharedKotlinIntProgression, InkwellSharedKotlinIntProgressionCompanion, InkwellSharedKotlinIntRange, InkwellSharedKotlinIntRangeCompanion, InkwellSharedKotlinNothing, InkwellSharedKotlinPair<__covariant A, __covariant B>, InkwellSharedKotlinRuntimeException, InkwellSharedKotlinThrowable, InkwellSharedKotlinx_serialization_coreSerialKind, InkwellSharedKotlinx_serialization_coreSerializersModule, InkwellSharedKotlinx_serialization_jsonJsonElement, InkwellSharedKotlinx_serialization_jsonJsonElementCompanion, InkwellSharedLeafletContentConverter, InkwellSharedLeafletFacet, InkwellSharedLeafletFacetFeature, InkwellSharedLeafletTypes, InkwellSharedLegacyPalette, InkwellSharedLegalDocuments, InkwellSharedMarkdownBlock, InkwellSharedMarkdownBlockBlockquote, InkwellSharedMarkdownBlockCode, InkwellSharedMarkdownBlockHeading, InkwellSharedMarkdownBlockHorizontalRule, InkwellSharedMarkdownBlockImage, InkwellSharedMarkdownBlockMath, InkwellSharedMarkdownBlockOrderedList, InkwellSharedMarkdownBlockParagraph, InkwellSharedMarkdownBlockTaskList, InkwellSharedMarkdownBlockUnorderedList, InkwellSharedMarkdownListItem, InkwellSharedMarkdownParser, InkwellSharedMarkdownSerializer, InkwellSharedMarkpubContentConverter, InkwellSharedMarkpubTypes, InkwellSharedNotificationPolicy, InkwellSharedNotificationStyleNone, InkwellSharedNotificationStyleSingle, InkwellSharedNotificationStyleSummary, InkwellSharedNumberFormat, InkwellSharedOAuthScopes, InkwellSharedOffprintContentConverter, InkwellSharedOffprintTypes, InkwellSharedPcktContentConverter, InkwellSharedPcktTypes, InkwellSharedPublicationMatcher, InkwellSharedPublicationPreferences, InkwellSharedPublicationTheme, InkwellSharedRecordListPolicy, InkwellSharedRgbColor, InkwellSharedRgbaColor, InkwellSharedSearchBackendUrl, InkwellSharedSearchResultClassifier, InkwellSharedSharedConvertResult, InkwellSharedSharedDocumentRecord, InkwellSharedSharedGraphRecommend, InkwellSharedSharedGraphSubscription, InkwellSharedSharedLeafletComment, InkwellSharedSharedLeafletCommentReplyRef, InkwellSharedSharedPublicationRecord, InkwellSharedSharedReaderTheme, InkwellSharedSharedReaderThemeCompanion, InkwellSharedSharedReaderThemeFontFamily, InkwellSharedSharedWriteResult, InkwellSharedStringUtils, InkwellSharedStrongRef, InkwellSharedSupportersList, InkwellSharedTipPromptPolicy, InkwellSharedUrlUtils, InkwellSharedUserInputLexicon, InkwellSharedUtf8Offsets, InkwellSharedVerificationFailure, InkwellSharedVerificationFailureDocumentLinkMissing, InkwellSharedVerificationFailureEndpointUnreachable, InkwellSharedVerificationFailureInvalidDocumentURL, InkwellSharedVerificationFailureInvalidPublicationURL, InkwellSharedVerificationFailureMalformedResponse, InkwellSharedVerificationFailureMismatchedURI, InkwellSharedVerificationFailureUnexpected, InkwellSharedVerificationResult, InkwellSharedVerificationResultFailed, InkwellSharedVerificationResultVerified, InkwellSharedVerificationUrls, InkwellSharedXrpcEndpoints, RichTextFacet, RichTextFeature;

@protocol InkwellSharedKotlinAnnotation, InkwellSharedKotlinClosedRange, InkwellSharedKotlinComparable, InkwellSharedKotlinFunction, InkwellSharedKotlinIterable, InkwellSharedKotlinIterator, InkwellSharedKotlinKAnnotatedElement, InkwellSharedKotlinKClass, InkwellSharedKotlinKClassifier, InkwellSharedKotlinKDeclarationContainer, InkwellSharedKotlinOpenEndRange, InkwellSharedKotlinSuspendFunction2, InkwellSharedKotlinx_serialization_coreCompositeDecoder, InkwellSharedKotlinx_serialization_coreCompositeEncoder, InkwellSharedKotlinx_serialization_coreDecoder, InkwellSharedKotlinx_serialization_coreDeserializationStrategy, InkwellSharedKotlinx_serialization_coreEncoder, InkwellSharedKotlinx_serialization_coreKSerializer, InkwellSharedKotlinx_serialization_coreSerialDescriptor, InkwellSharedKotlinx_serialization_coreSerializationStrategy, InkwellSharedKotlinx_serialization_coreSerializersModuleCollector, InkwellSharedNotificationStyle;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((swift_name("KotlinBase")))
@interface InkwellSharedBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface InkwellSharedBase (InkwellSharedBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface InkwellSharedMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface InkwellSharedMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorInkwellSharedKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface InkwellSharedNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end

__attribute__((swift_name("KotlinByte")))
@interface InkwellSharedByte : InkwellSharedNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface InkwellSharedUByte : InkwellSharedNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface InkwellSharedShort : InkwellSharedNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface InkwellSharedUShort : InkwellSharedNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface InkwellSharedInt : InkwellSharedNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface InkwellSharedUInt : InkwellSharedNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface InkwellSharedLong : InkwellSharedNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface InkwellSharedULong : InkwellSharedNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface InkwellSharedFloat : InkwellSharedNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface InkwellSharedDouble : InkwellSharedNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface InkwellSharedBoolean : InkwellSharedNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end


/**
 * Parsed AT-URI: did + collection + recordKey extracted from the
 * standard `at://did/collection/rkey` format.
 *
 * Mirrors the iOS `ATURI` struct in `StandardSiteTypes.swift` and the
 * Android `AtUri` data class in `Models.kt` — identical parse semantics.
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AtUri")))
@interface InkwellSharedAtUri : InkwellSharedBase
- (instancetype)initWithDid:(NSString *)did collection:(NSString *)collection recordKey:(NSString *)recordKey __attribute__((swift_name("init(did:collection:recordKey:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) InkwellSharedAtUriCompanion *companion __attribute__((swift_name("companion")));
- (InkwellSharedAtUri *)doCopyDid:(NSString *)did collection:(NSString *)collection recordKey:(NSString *)recordKey __attribute__((swift_name("doCopy(did:collection:recordKey:)")));

/**
 * Parsed AT-URI: did + collection + recordKey extracted from the
 * standard `at://did/collection/rkey` format.
 *
 * Mirrors the iOS `ATURI` struct in `StandardSiteTypes.swift` and the
 * Android `AtUri` data class in `Models.kt` — identical parse semantics.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Parsed AT-URI: did + collection + recordKey extracted from the
 * standard `at://did/collection/rkey` format.
 *
 * Mirrors the iOS `ATURI` struct in `StandardSiteTypes.swift` and the
 * Android `AtUri` data class in `Models.kt` — identical parse semantics.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Parsed AT-URI: did + collection + recordKey extracted from the
 * standard `at://did/collection/rkey` format.
 *
 * Mirrors the iOS `ATURI` struct in `StandardSiteTypes.swift` and the
 * Android `AtUri` data class in `Models.kt` — identical parse semantics.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *collection __attribute__((swift_name("collection")));
@property (readonly) NSString *did __attribute__((swift_name("did")));
@property (readonly) NSString *recordKey __attribute__((swift_name("recordKey")));

/** Reassembles the canonical AT-URI string. */
@property (readonly) NSString *uri __attribute__((swift_name("uri")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AtUri.Companion")))
@interface InkwellSharedAtUriCompanion : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedAtUriCompanion *shared __attribute__((swift_name("shared")));

/** Parses at:// URIs. Returns null for malformed input. */
- (InkwellSharedAtUri * _Nullable)parseUri:(NSString *)uri __attribute__((swift_name("parse(uri:)")));
- (id<InkwellSharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ConstellationBacklink")))
@interface InkwellSharedConstellationBacklink : InkwellSharedBase
- (instancetype)initWithDid:(NSString *)did collection:(NSString *)collection rkey:(NSString *)rkey __attribute__((swift_name("init(did:collection:rkey:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedConstellationBacklink *)doCopyDid:(NSString *)did collection:(NSString *)collection rkey:(NSString *)rkey __attribute__((swift_name("doCopy(did:collection:rkey:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *collection __attribute__((swift_name("collection")));
@property (readonly) NSString *did __attribute__((swift_name("did")));
@property (readonly) NSString *recordURI __attribute__((swift_name("recordURI")));
@property (readonly) NSString *rkey __attribute__((swift_name("rkey")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ConstellationPagination")))
@interface InkwellSharedConstellationPagination : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)constellationPagination __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedConstellationPagination *shared __attribute__((swift_name("shared")));

/**
 * Deduplicates backlinks by (did, rkey) since a single record could
 * appear in multiple source paths.
 */
- (NSArray<InkwellSharedConstellationBacklink *> *)deduplicateBacklinks:(NSArray<InkwellSharedConstellationBacklink *> *)backlinks __attribute__((swift_name("deduplicate(backlinks:)")));

/**
 * Generic cursor-based pagination over a backlink source.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)paginateBacklinksFetchPage:(id<InkwellSharedKotlinSuspendFunction2>)fetchPage maxCount:(int32_t)maxCount completionHandler:(void (^)(NSArray<InkwellSharedConstellationBacklink *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("paginateBacklinks(fetchPage:maxCount:completionHandler:)")));

/**
 * Total recommend count for a document, across the whole network.
 *
 * Requests a single record first: if there's no next cursor the whole
 * result set fit in that page, so its size is the count. Otherwise
 * falls back to full pagination.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)recommendCountFetchPage:(id<InkwellSharedKotlinSuspendFunction2>)fetchPage completionHandler:(void (^)(InkwellSharedInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("recommendCount(fetchPage:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ConstellationResponse")))
@interface InkwellSharedConstellationResponse : InkwellSharedBase
- (instancetype)initWithRecords:(NSArray<InkwellSharedConstellationBacklink *> *)records cursor:(NSString * _Nullable)cursor __attribute__((swift_name("init(records:cursor:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedConstellationResponse *)doCopyRecords:(NSArray<InkwellSharedConstellationBacklink *> *)records cursor:(NSString * _Nullable)cursor __attribute__((swift_name("doCopy(records:cursor:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable cursor __attribute__((swift_name("cursor")));
@property (readonly) NSArray<InkwellSharedConstellationBacklink *> *records __attribute__((swift_name("records")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ConstellationSourcePaths")))
@interface InkwellSharedConstellationSourcePaths : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)constellationSourcePaths __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedConstellationSourcePaths *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *EMBED_EXTERNAL_URI __attribute__((swift_name("EMBED_EXTERNAL_URI")));
@property (readonly) NSString *MENTION_FACET_LINK __attribute__((swift_name("MENTION_FACET_LINK")));
@end


/**
 * Shared loss label maps per content format.
 *
 * Maps block type strings that can't be represented as markdown to
 * human-readable labels shown to the user. Centralises the labels
 * currently duplicated across iOS ContentProvider.swift and Android
 * PcktOffprintConverter.kt / MarkdownConverter.kt.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlockLossLabels")))
@interface InkwellSharedBlockLossLabels : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Shared loss label maps per content format.
 *
 * Maps block type strings that can't be represented as markdown to
 * human-readable labels shown to the user. Centralises the labels
 * currently duplicated across iOS ContentProvider.swift and Android
 * PcktOffprintConverter.kt / MarkdownConverter.kt.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)blockLossLabels __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedBlockLossLabels *shared __attribute__((swift_name("shared")));
@property (readonly) NSDictionary<NSString *, NSString *> *leaflet __attribute__((swift_name("leaflet")));
@property (readonly) NSDictionary<NSString *, NSString *> *offprint __attribute__((swift_name("offprint")));
@property (readonly) NSDictionary<NSString *, NSString *> *pckt __attribute__((swift_name("pckt")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlueskyEmbedTypes")))
@interface InkwellSharedBlueskyEmbedTypes : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)blueskyEmbedTypes __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedBlueskyEmbedTypes *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *EXTERNAL __attribute__((swift_name("EXTERNAL")));
@property (readonly) NSString *IMAGES __attribute__((swift_name("IMAGES")));
@property (readonly) NSString *RECORD __attribute__((swift_name("RECORD")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CdnUrls")))
@interface InkwellSharedCdnUrls : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cdnUrls __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedCdnUrls *shared __attribute__((swift_name("shared")));
- (NSString *)bskyThumbnailDid:(NSString *)did link:(NSString *)link __attribute__((swift_name("bskyThumbnail(did:link:)")));
@end


/**
 * Shared content format type detection.
 *
 * Maps AT Protocol record `$type` strings to known content formats.
 * Both platforms use these constants to dispatch rendering.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ContentFormatDetector")))
@interface InkwellSharedContentFormatDetector : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Shared content format type detection.
 *
 * Maps AT Protocol record `$type` strings to known content formats.
 * Both platforms use these constants to dispatch rendering.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)contentFormatDetector __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedContentFormatDetector *shared __attribute__((swift_name("shared")));

/**
 * Returns true if [type] is a recognised content format.
 */
- (BOOL)isKnownType:(NSString * _Nullable)type __attribute__((swift_name("isKnown(type:)")));

/**
 * Returns true if [type] is a pckt or Offprint format
 * (both use the same block-array converter).
 */
- (BOOL)isPcktOrOffprintType:(NSString * _Nullable)type __attribute__((swift_name("isPcktOrOffprint(type:)")));

/**
 * All known content format type strings.
 */
@property (readonly) NSArray<NSString *> *ALL __attribute__((swift_name("ALL")));
@property (readonly) NSString *LEAFLET __attribute__((swift_name("LEAFLET")));
@property (readonly) NSString *MARKPUB __attribute__((swift_name("MARKPUB")));
@property (readonly) NSString *OFFPRINT __attribute__((swift_name("OFFPRINT")));
@property (readonly) NSString *PCKT __attribute__((swift_name("PCKT")));
@end


/**
 * Dispatches content conversion to the correct format-specific converter.
 *
 * This is the main entry point for shared content conversion. Both platforms
 * call these functions instead of maintaining their own conversion logic.
 *
 * Mirrors iOS `ProviderRegistry` and Android `MarkdownConverter.convert`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ContentFormatDispatcher")))
@interface InkwellSharedContentFormatDispatcher : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Dispatches content conversion to the correct format-specific converter.
 *
 * This is the main entry point for shared content conversion. Both platforms
 * call these functions instead of maintaining their own conversion logic.
 *
 * Mirrors iOS `ProviderRegistry` and Android `MarkdownConverter.convert`.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)contentFormatDispatcher __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedContentFormatDispatcher *shared __attribute__((swift_name("shared")));

/**
 * Returns the content type string for a format name.
 */
- (NSString * _Nullable)contentTypeForFormatFormat:(NSString *)format __attribute__((swift_name("contentTypeForFormat(format:)")));

/**
 * Returns the format name for a content type string.
 */
- (NSString * _Nullable)formatForContentTypeType:(NSString * _Nullable)type __attribute__((swift_name("formatForContentType(type:)")));

/**
 * Converts markdown text to a format-specific content map.
 *
 * @param markdown The markdown source text.
 * @param format The format identifier ("Leaflet", "Markpub", "pckt", "Offprint").
 * @param uploadedBlobs Map of CID → blob JSON for image round-tripping.
 * @return A [SharedWriteResult] with the content map and any lost content.
 */
- (InkwellSharedSharedWriteResult *)fromMarkdownMarkdown:(NSString *)markdown format:(NSString *)format uploadedBlobs:(NSDictionary<NSString *, NSDictionary<NSString *, id> *> *)uploadedBlobs __attribute__((swift_name("fromMarkdown(markdown:format:uploadedBlobs:)")));

/**
 * Returns true if the given content type is a pckt or Offprint format.
 */
- (BOOL)isPcktOrOffprintType:(NSString * _Nullable)type __attribute__((swift_name("isPcktOrOffprint(type:)")));

/**
 * Converts a content map to markdown, detecting the format from `$type`.
 *
 * @param content The content map with a `$type` field.
 * @param authorDid The author's DID, used for CDN image URL resolution.
 * @return A [SharedConvertResult] with the markdown blocks and any lost content.
 */
- (InkwellSharedSharedConvertResult *)toMarkdownContent:(NSDictionary<NSString *, id> *)content authorDid:(NSString *)authorDid __attribute__((swift_name("toMarkdown(content:authorDid:)")));

/**
 * Converts a content map to a raw markdown string.
 *
 * @param content The content map with a `$type` field.
 * @param authorDid The author's DID, used for CDN image URL resolution.
 * @return The markdown string, or empty if conversion fails.
 */
- (NSString *)toMarkdownStringContent:(NSDictionary<NSString *, id> *)content authorDid:(NSString *)authorDid __attribute__((swift_name("toMarkdownString(content:authorDid:)")));
@end


/**
 * Converts between generic Map-based content representations (used by shared
 * converters) and kotlinx.serialization JsonObjects (used by Android).
 *
 * This bridge lets the shared KMP converters remain JSON-library-agnostic
 * while the Android platform layer keeps its existing kotlinx.serialization
 * contract.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("JsonMapBridge")))
@interface InkwellSharedJsonMapBridge : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Converts between generic Map-based content representations (used by shared
 * converters) and kotlinx.serialization JsonObjects (used by Android).
 *
 * This bridge lets the shared KMP converters remain JSON-library-agnostic
 * while the Android platform layer keeps its existing kotlinx.serialization
 * contract.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)jsonMapBridge __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedJsonMapBridge *shared __attribute__((swift_name("shared")));

/**
 * Recursively converts a [JsonObject] to a [Map].
 */
- (NSDictionary<NSString *, id> *)jsonToMapObj:(NSDictionary<NSString *, InkwellSharedKotlinx_serialization_jsonJsonElement *> *)obj __attribute__((swift_name("jsonToMap(obj:)")));

/**
 * Recursively converts a [Map] tree to a [JsonObject].
 */
- (NSDictionary<NSString *, InkwellSharedKotlinx_serialization_jsonJsonElement *> *)mapToJsonMap:(NSDictionary<NSString *, id> *)map __attribute__((swift_name("mapToJson(map:)")));
@end


/**
 * Converts between Leaflet content (`pub.leaflet.content`) and markdown blocks.
 *
 * Leaflet documents are a list of pages; we read and write a single
 * `linearDocument` page whose blocks map closely to markdown. Inline
 * formatting uses Leaflet's richtext facets.
 *
 * Mirrors iOS `LeafletProvider` and Android `MarkdownConverter.leafletBlockToJson`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LeafletContentConverter")))
@interface InkwellSharedLeafletContentConverter : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Converts between Leaflet content (`pub.leaflet.content`) and markdown blocks.
 *
 * Leaflet documents are a list of pages; we read and write a single
 * `linearDocument` page whose blocks map closely to markdown. Inline
 * formatting uses Leaflet's richtext facets.
 *
 * Mirrors iOS `LeafletProvider` and Android `MarkdownConverter.leafletBlockToJson`.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)leafletContentConverter __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedLeafletContentConverter *shared __attribute__((swift_name("shared")));

/**
 * Converts markdown text to a Leaflet content map.
 *
 * @param markdown The markdown source text.
 * @param uploadedBlobs Map of CID → blob JSON for image round-tripping.
 */
- (InkwellSharedSharedWriteResult *)fromMarkdownMarkdown:(NSString *)markdown uploadedBlobs:(NSDictionary<NSString *, NSDictionary<NSString *, id> *> *)uploadedBlobs __attribute__((swift_name("fromMarkdown(markdown:uploadedBlobs:)")));

/**
 * Converts a Leaflet content map to a [SharedConvertResult].
 *
 * The content map is expected to have the shape:
 * ```
 * { "$type": "pub.leaflet.content", "pages": [ { "blocks": [ ... ] } ] }
 * ```
 *
 * Each block is a map with a `$type` key and format-specific fields.
 */
- (InkwellSharedSharedConvertResult *)toMarkdownContent:(NSDictionary<NSString *, id> *)content __attribute__((swift_name("toMarkdown(content:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LeafletTypes")))
@interface InkwellSharedLeafletTypes : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)leafletTypes __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedLeafletTypes *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *BLOCKS_BLOCKQUOTE __attribute__((swift_name("BLOCKS_BLOCKQUOTE")));
@property (readonly) NSString *BLOCKS_BSKY_POST __attribute__((swift_name("BLOCKS_BSKY_POST")));
@property (readonly) NSString *BLOCKS_BUTTON __attribute__((swift_name("BLOCKS_BUTTON")));
@property (readonly) NSString *BLOCKS_CHECKLIST __attribute__((swift_name("BLOCKS_CHECKLIST")));
@property (readonly) NSString *BLOCKS_CODE __attribute__((swift_name("BLOCKS_CODE")));
@property (readonly) NSString *BLOCKS_DIVIDER __attribute__((swift_name("BLOCKS_DIVIDER")));
@property (readonly) NSString *BLOCKS_HEADER __attribute__((swift_name("BLOCKS_HEADER")));
@property (readonly) NSString *BLOCKS_HORIZONTAL_RULE __attribute__((swift_name("BLOCKS_HORIZONTAL_RULE")));
@property (readonly) NSString *BLOCKS_IFRAME __attribute__((swift_name("BLOCKS_IFRAME")));
@property (readonly) NSString *BLOCKS_IMAGE __attribute__((swift_name("BLOCKS_IMAGE")));
@property (readonly) NSString *BLOCKS_MATH __attribute__((swift_name("BLOCKS_MATH")));
@property (readonly) NSString *BLOCKS_ORDERED_LIST __attribute__((swift_name("BLOCKS_ORDERED_LIST")));
@property (readonly) NSString *BLOCKS_PAGE __attribute__((swift_name("BLOCKS_PAGE")));
@property (readonly) NSString *BLOCKS_PARAGRAPH __attribute__((swift_name("BLOCKS_PARAGRAPH")));
@property (readonly) NSString *BLOCKS_POLL __attribute__((swift_name("BLOCKS_POLL")));
@property (readonly) NSString *BLOCKS_POSTS_LIST __attribute__((swift_name("BLOCKS_POSTS_LIST")));
@property (readonly) NSString *BLOCKS_SIGNUP __attribute__((swift_name("BLOCKS_SIGNUP")));
@property (readonly) NSString *BLOCKS_STANDARD_SITE_POST __attribute__((swift_name("BLOCKS_STANDARD_SITE_POST")));
@property (readonly) NSString *BLOCKS_TEXT __attribute__((swift_name("BLOCKS_TEXT")));
@property (readonly) NSString *BLOCKS_UNORDERED_LIST __attribute__((swift_name("BLOCKS_UNORDERED_LIST")));
@property (readonly) NSString *BLOCKS_WEBSITE __attribute__((swift_name("BLOCKS_WEBSITE")));
@property (readonly) NSString *CONTENT __attribute__((swift_name("CONTENT")));
@property (readonly) NSString *LIST_ITEM_ORDERED __attribute__((swift_name("LIST_ITEM_ORDERED")));
@property (readonly) NSString *LIST_ITEM_UNORDERED __attribute__((swift_name("LIST_ITEM_UNORDERED")));
@property (readonly) NSString *PAGES_LINEAR_DOCUMENT __attribute__((swift_name("PAGES_LINEAR_DOCUMENT")));
@end


/**
 * Converts between Markpub content (`at.markpub.markdown`) and markdown blocks.
 *
 * Markpub stores GFM markdown directly, so conversion is near-identity:
 * read the inline `text.markdown` and write it straight back. Nothing is
 * ever lost.
 *
 * Mirrors iOS `MarkpubProvider` and Android `MarkdownConverter.buildMarkpubContent`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkpubContentConverter")))
@interface InkwellSharedMarkpubContentConverter : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Converts between Markpub content (`at.markpub.markdown`) and markdown blocks.
 *
 * Markpub stores GFM markdown directly, so conversion is near-identity:
 * read the inline `text.markdown` and write it straight back. Nothing is
 * ever lost.
 *
 * Mirrors iOS `MarkpubProvider` and Android `MarkdownConverter.buildMarkpubContent`.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)markpubContentConverter __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedMarkpubContentConverter *shared __attribute__((swift_name("shared")));

/**
 * Converts markdown text to a Markpub content map.
 *
 * @param markdown The markdown source text.
 */
- (InkwellSharedSharedWriteResult *)fromMarkdownMarkdown:(NSString *)markdown __attribute__((swift_name("fromMarkdown(markdown:)")));

/**
 * Converts a Markpub content map to a [SharedConvertResult].
 *
 * The content map is expected to have the shape:
 * ```
 * { "$type": "at.markpub.markdown", "text": { "$type": "at.markpub.text", "markdown": "..." } }
 * ```
 */
- (InkwellSharedSharedConvertResult *)toMarkdownContent:(NSDictionary<NSString *, id> *)content __attribute__((swift_name("toMarkdown(content:)")));

/**
 * Extracts the raw markdown string from a Markpub content map.
 * Use this when you need the markdown text directly without block parsing.
 */
- (NSString *)toRawMarkdownContent:(NSDictionary<NSString *, id> *)content __attribute__((swift_name("toRawMarkdown(content:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkpubTypes")))
@interface InkwellSharedMarkpubTypes : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)markpubTypes __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedMarkpubTypes *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *CONTENT __attribute__((swift_name("CONTENT")));
@property (readonly) NSString *TEXT __attribute__((swift_name("TEXT")));
@end


/**
 * Converts between Offprint content (`app.offprint.content`) and markdown blocks.
 *
 * Offprint stores an `items` array of blocks. Blocks map closely to markdown;
 * inline formatting uses Offprint's richtext facets. Headings are capped at
 * level 3. Math blocks are supported natively. Blockquotes wrap inner text
 * in a `content` array. Lists use `children` arrays.
 *
 * Mirrors iOS `OffprintProvider` and Android `MarkdownConverter.offprintBlockToJson`
 * / `PcktOffprintConverter`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OffprintContentConverter")))
@interface InkwellSharedOffprintContentConverter : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Converts between Offprint content (`app.offprint.content`) and markdown blocks.
 *
 * Offprint stores an `items` array of blocks. Blocks map closely to markdown;
 * inline formatting uses Offprint's richtext facets. Headings are capped at
 * level 3. Math blocks are supported natively. Blockquotes wrap inner text
 * in a `content` array. Lists use `children` arrays.
 *
 * Mirrors iOS `OffprintProvider` and Android `MarkdownConverter.offprintBlockToJson`
 * / `PcktOffprintConverter`.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)offprintContentConverter __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedOffprintContentConverter *shared __attribute__((swift_name("shared")));

/**
 * Converts markdown text to an Offprint content map.
 *
 * @param markdown The markdown source text.
 * @param uploadedBlobs Map of CID → blob JSON for image round-tripping.
 */
- (InkwellSharedSharedWriteResult *)fromMarkdownMarkdown:(NSString *)markdown uploadedBlobs:(NSDictionary<NSString *, NSDictionary<NSString *, id> *> *)uploadedBlobs __attribute__((swift_name("fromMarkdown(markdown:uploadedBlobs:)")));

/**
 * Converts an Offprint content map to a [SharedConvertResult].
 *
 * The content map is expected to have the shape:
 * ```
 * { "$type": "app.offprint.content", "items": [ ... ] }
 * ```
 */
- (InkwellSharedSharedConvertResult *)toMarkdownContent:(NSDictionary<NSString *, id> *)content __attribute__((swift_name("toMarkdown(content:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OffprintTypes")))
@interface InkwellSharedOffprintTypes : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)offprintTypes __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedOffprintTypes *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *BLOCK_BLOCKQUOTE __attribute__((swift_name("BLOCK_BLOCKQUOTE")));
@property (readonly) NSString *BLOCK_BLUESKY_POST __attribute__((swift_name("BLOCK_BLUESKY_POST")));
@property (readonly) NSString *BLOCK_BULLET_LIST __attribute__((swift_name("BLOCK_BULLET_LIST")));
@property (readonly) NSString *BLOCK_BUTTON __attribute__((swift_name("BLOCK_BUTTON")));
@property (readonly) NSString *BLOCK_CALLOUT __attribute__((swift_name("BLOCK_CALLOUT")));
@property (readonly) NSString *BLOCK_CODE_BLOCK __attribute__((swift_name("BLOCK_CODE_BLOCK")));
@property (readonly) NSString *BLOCK_HEADING __attribute__((swift_name("BLOCK_HEADING")));
@property (readonly) NSString *BLOCK_HORIZONTAL_RULE __attribute__((swift_name("BLOCK_HORIZONTAL_RULE")));
@property (readonly) NSString *BLOCK_IMAGE __attribute__((swift_name("BLOCK_IMAGE")));
@property (readonly) NSString *BLOCK_IMAGE_CAROUSEL __attribute__((swift_name("BLOCK_IMAGE_CAROUSEL")));
@property (readonly) NSString *BLOCK_IMAGE_DIFF __attribute__((swift_name("BLOCK_IMAGE_DIFF")));
@property (readonly) NSString *BLOCK_IMAGE_GRID __attribute__((swift_name("BLOCK_IMAGE_GRID")));
@property (readonly) NSString *BLOCK_MATH_BLOCK __attribute__((swift_name("BLOCK_MATH_BLOCK")));
@property (readonly) NSString *BLOCK_PREFIX __attribute__((swift_name("BLOCK_PREFIX")));
@property (readonly) NSString *BLOCK_TASK_LIST __attribute__((swift_name("BLOCK_TASK_LIST")));
@property (readonly) NSString *BLOCK_TEXT __attribute__((swift_name("BLOCK_TEXT")));
@property (readonly) NSString *BLOCK_WEB_BOOKMARK __attribute__((swift_name("BLOCK_WEB_BOOKMARK")));
@property (readonly) NSString *BLOCK_WEB_EMBED __attribute__((swift_name("BLOCK_WEB_EMBED")));
@property (readonly) NSString *CONTENT __attribute__((swift_name("CONTENT")));
@end


/**
 * Converts between pckt content (`blog.pckt.content`) and markdown blocks.
 *
 * Pckt stores an `items` array of blocks. Blocks map closely to markdown;
 * inline formatting uses pckt's richtext facets. Blockquotes wrap inner
 * text in a `content` array containing a text block. Lists use `content`
 * arrays with nested sub-lists.
 *
 * Mirrors iOS `PcktProvider` and Android `MarkdownConverter.pcktBlockToJson`
 * / `PcktOffprintConverter`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PcktContentConverter")))
@interface InkwellSharedPcktContentConverter : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Converts between pckt content (`blog.pckt.content`) and markdown blocks.
 *
 * Pckt stores an `items` array of blocks. Blocks map closely to markdown;
 * inline formatting uses pckt's richtext facets. Blockquotes wrap inner
 * text in a `content` array containing a text block. Lists use `content`
 * arrays with nested sub-lists.
 *
 * Mirrors iOS `PcktProvider` and Android `MarkdownConverter.pcktBlockToJson`
 * / `PcktOffprintConverter`.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)pcktContentConverter __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedPcktContentConverter *shared __attribute__((swift_name("shared")));

/**
 * Converts markdown text to a pckt content map.
 *
 * @param markdown The markdown source text.
 * @param uploadedBlobs Map of CID → blob JSON for image round-tripping.
 */
- (InkwellSharedSharedWriteResult *)fromMarkdownMarkdown:(NSString *)markdown uploadedBlobs:(NSDictionary<NSString *, NSDictionary<NSString *, id> *> *)uploadedBlobs __attribute__((swift_name("fromMarkdown(markdown:uploadedBlobs:)")));

/**
 * Converts a pckt content map to a [SharedConvertResult].
 *
 * The content map is expected to have the shape:
 * ```
 * { "$type": "blog.pckt.content", "items": [ ... ] }
 * ```
 */
- (InkwellSharedSharedConvertResult *)toMarkdownContent:(NSDictionary<NSString *, id> *)content __attribute__((swift_name("toMarkdown(content:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PcktTypes")))
@interface InkwellSharedPcktTypes : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)pcktTypes __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedPcktTypes *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *BLOCK_BLOCKQUOTE __attribute__((swift_name("BLOCK_BLOCKQUOTE")));
@property (readonly) NSString *BLOCK_BLUESKY_EMBED __attribute__((swift_name("BLOCK_BLUESKY_EMBED")));
@property (readonly) NSString *BLOCK_BULLET_LIST __attribute__((swift_name("BLOCK_BULLET_LIST")));
@property (readonly) NSString *BLOCK_CODE_BLOCK __attribute__((swift_name("BLOCK_CODE_BLOCK")));
@property (readonly) NSString *BLOCK_GALLERY __attribute__((swift_name("BLOCK_GALLERY")));
@property (readonly) NSString *BLOCK_HARD_BREAK __attribute__((swift_name("BLOCK_HARD_BREAK")));
@property (readonly) NSString *BLOCK_HEADING __attribute__((swift_name("BLOCK_HEADING")));
@property (readonly) NSString *BLOCK_HORIZONTAL_RULE __attribute__((swift_name("BLOCK_HORIZONTAL_RULE")));
@property (readonly) NSString *BLOCK_IFRAME __attribute__((swift_name("BLOCK_IFRAME")));
@property (readonly) NSString *BLOCK_IMAGE __attribute__((swift_name("BLOCK_IMAGE")));
@property (readonly) NSString *BLOCK_LIST_ITEM __attribute__((swift_name("BLOCK_LIST_ITEM")));
@property (readonly) NSString *BLOCK_MATH_BLOCK __attribute__((swift_name("BLOCK_MATH_BLOCK")));
@property (readonly) NSString *BLOCK_MENTION __attribute__((swift_name("BLOCK_MENTION")));
@property (readonly) NSString *BLOCK_NOTE_EMBED __attribute__((swift_name("BLOCK_NOTE_EMBED")));
@property (readonly) NSString *BLOCK_PREFIX __attribute__((swift_name("BLOCK_PREFIX")));
@property (readonly) NSString *BLOCK_TABLE __attribute__((swift_name("BLOCK_TABLE")));
@property (readonly) NSString *BLOCK_TASK_LIST __attribute__((swift_name("BLOCK_TASK_LIST")));
@property (readonly) NSString *BLOCK_TEXT __attribute__((swift_name("BLOCK_TEXT")));
@property (readonly) NSString *BLOCK_WEBSITE __attribute__((swift_name("BLOCK_WEBSITE")));
@property (readonly) NSString *CONTENT __attribute__((swift_name("CONTENT")));
@end


/**
 * Shared publication/document matching logic.
 *
 * Determines whether a document belongs to a publication by comparing
 * the document's site against the publication's AT-URI and URL.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PublicationMatcher")))
@interface InkwellSharedPublicationMatcher : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Shared publication/document matching logic.
 *
 * Determines whether a document belongs to a publication by comparing
 * the document's site against the publication's AT-URI and URL.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)publicationMatcher __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedPublicationMatcher *shared __attribute__((swift_name("shared")));

/**
 * Returns true if [documentSite] belongs to the publication identified by
 * [publicationUri] and [publicationUrl].
 *
 * Matches on:
 * - Exact AT-URI equality: `documentSite == publicationUri`
 * - Normalized URL equality: handles case differences and trailing slashes
 * - Subpath prefix: `documentSite` starts with `publicationUrl/`
 */
- (BOOL)documentBelongsToPublicationDocumentSite:(NSString *)documentSite publicationUri:(NSString *)publicationUri publicationUrl:(NSString * _Nullable)publicationUrl __attribute__((swift_name("documentBelongsToPublication(documentSite:publicationUri:publicationUrl:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchBackendUrl")))
@interface InkwellSharedSearchBackendUrl : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)searchBackendUrl __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedSearchBackendUrl *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *BASE __attribute__((swift_name("BASE")));
@end


/**
 * Shared classification and URL construction for search/discovery results.
 *
 * Both platforms use the same logic to determine whether a search result
 * is a publication or a Standard.site document, and to construct the
 * canonical web URL for navigation.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchResultClassifier")))
@interface InkwellSharedSearchResultClassifier : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Shared classification and URL construction for search/discovery results.
 *
 * Both platforms use the same logic to determine whether a search result
 * is a publication or a Standard.site document, and to construct the
 * canonical web URL for navigation.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)searchResultClassifier __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedSearchResultClassifier *shared __attribute__((swift_name("shared")));

/**
 * Returns true if [type] indicates a publication record.
 */
- (BOOL)isPublicationType:(NSString *)type __attribute__((swift_name("isPublication(type:)")));

/**
 * Returns true if [uri] is a `site.standard.document` record.
 */
- (BOOL)isStandardSiteDocumentUri:(NSString *)uri __attribute__((swift_name("isStandardSiteDocument(uri:)")));

/**
 * Constructs the canonical web URL for a search result.
 *
 * Returns null if [basePath] is null or empty.
 *
 * - Publications link to the origin directly.
 * - Documents with a [path] link to `origin + path`.
 * - Leaflet documents without a path fall back to `origin + rkey`.
 */
- (NSString * _Nullable)webURLBasePath:(NSString * _Nullable)basePath path:(NSString * _Nullable)path rkey:(NSString * _Nullable)rkey platform:(NSString * _Nullable)platform isPublication:(BOOL)isPublication __attribute__((swift_name("webURL(basePath:path:rkey:platform:isPublication:)")));
@property (readonly) NSString *LEAFLET_PLATFORM __attribute__((swift_name("LEAFLET_PLATFORM")));
@property (readonly) NSString *PUBLICATION_TYPE __attribute__((swift_name("PUBLICATION_TYPE")));
@property (readonly) NSString *SITE_STANDARD_DOCUMENT __attribute__((swift_name("SITE_STANDARD_DOCUMENT")));
@end


/**
 * Result of converting stored content to markdown.
 *
 * Mirrors iOS `ConvertResult` and Android `ConvertResult` in PcktOffprintConverter.kt.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedConvertResult")))
@interface InkwellSharedSharedConvertResult : InkwellSharedBase
- (instancetype)initWithBlocks:(NSArray<InkwellSharedMarkdownBlock *> *)blocks lost:(NSSet<NSString *> *)lost __attribute__((swift_name("init(blocks:lost:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedSharedConvertResult *)doCopyBlocks:(NSArray<InkwellSharedMarkdownBlock *> *)blocks lost:(NSSet<NSString *> *)lost __attribute__((swift_name("doCopy(blocks:lost:)")));

/**
 * Result of converting stored content to markdown.
 *
 * Mirrors iOS `ConvertResult` and Android `ConvertResult` in PcktOffprintConverter.kt.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Result of converting stored content to markdown.
 *
 * Mirrors iOS `ConvertResult` and Android `ConvertResult` in PcktOffprintConverter.kt.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Result of converting stored content to markdown.
 *
 * Mirrors iOS `ConvertResult` and Android `ConvertResult` in PcktOffprintConverter.kt.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<InkwellSharedMarkdownBlock *> *blocks __attribute__((swift_name("blocks")));
@property (readonly) NSSet<NSString *> *lost __attribute__((swift_name("lost")));
@end


/**
 * Result of converting markdown to format-specific content.
 *
 * Uses generic maps to represent JSON-like structures that both platforms
 * can consume: Android uses kotlinx.serialization `JsonObject`, iOS can
 * bridge from `[String: Any]` dictionaries.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedWriteResult")))
@interface InkwellSharedSharedWriteResult : InkwellSharedBase
- (instancetype)initWithContent:(NSDictionary<NSString *, id> *)content lost:(NSSet<NSString *> *)lost __attribute__((swift_name("init(content:lost:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedSharedWriteResult *)doCopyContent:(NSDictionary<NSString *, id> *)content lost:(NSSet<NSString *> *)lost __attribute__((swift_name("doCopy(content:lost:)")));

/**
 * Result of converting markdown to format-specific content.
 *
 * Uses generic maps to represent JSON-like structures that both platforms
 * can consume: Android uses kotlinx.serialization `JsonObject`, iOS can
 * bridge from `[String: Any]` dictionaries.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Result of converting markdown to format-specific content.
 *
 * Uses generic maps to represent JSON-like structures that both platforms
 * can consume: Android uses kotlinx.serialization `JsonObject`, iOS can
 * bridge from `[String: Any]` dictionaries.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Result of converting markdown to format-specific content.
 *
 * Uses generic maps to represent JSON-like structures that both platforms
 * can consume: Android uses kotlinx.serialization `JsonObject`, iOS can
 * bridge from `[String: Any]` dictionaries.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSDictionary<NSString *, id> *content __attribute__((swift_name("content")));
@property (readonly) NSSet<NSString *> *lost __attribute__((swift_name("lost")));
@end

__attribute__((objc_subclassing_restricted))
@interface FacetConverter : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)facetConverter __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) FacetConverter *shared __attribute__((swift_name("shared")));
- (NSString *)facetsToMarkdownPlaintext:(NSString *)plaintext facets:(NSArray<RichTextFacet *> * _Nullable)facets boldType:(NSString *)boldType italicType:(NSString *)italicType codeType:(NSString *)codeType strikeType:(NSString *)strikeType linkType:(NSString *)linkType lossy:(NSDictionary<NSString *, NSString *> *)lossy lost:(InkwellSharedMutableSet<NSString *> * _Nullable)lost __attribute__((swift_name("facetsToMarkdown(plaintext:facets:boldType:italicType:codeType:strikeType:linkType:lossy:lost:)")));
- (InkwellSharedKotlinPair<NSString *, NSArray<RichTextFacet *> *> *)markdownToFacetsMarkdown:(NSString *)markdown boldType:(NSString *)boldType italicType:(NSString *)italicType codeType:(NSString *)codeType strikeType:(NSString *)strikeType linkType:(NSString *)linkType __attribute__((swift_name("markdownToFacets(markdown:boldType:italicType:codeType:strikeType:linkType:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FacetDefinition")))
@interface InkwellSharedFacetDefinition : InkwellSharedBase
- (instancetype)initWithFacet:(NSString *)facet byteSlice:(NSString *)byteSlice bold:(NSString *)bold italic:(NSString *)italic code:(NSString *)code strike:(NSString *)strike link:(NSString *)link lossy:(NSDictionary<NSString *, NSString *> *)lossy __attribute__((swift_name("init(facet:byteSlice:bold:italic:code:strike:link:lossy:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedFacetDefinition *)doCopyFacet:(NSString *)facet byteSlice:(NSString *)byteSlice bold:(NSString *)bold italic:(NSString *)italic code:(NSString *)code strike:(NSString *)strike link:(NSString *)link lossy:(NSDictionary<NSString *, NSString *> *)lossy __attribute__((swift_name("doCopy(facet:byteSlice:bold:italic:code:strike:link:lossy:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *bold __attribute__((swift_name("bold")));
@property (readonly) NSString *byteSlice __attribute__((swift_name("byteSlice")));
@property (readonly) NSString *code __attribute__((swift_name("code")));
@property (readonly) NSString *facet __attribute__((swift_name("facet")));
@property (readonly) NSString *italic __attribute__((swift_name("italic")));
@property (readonly) NSString *link __attribute__((swift_name("link")));
@property (readonly) NSDictionary<NSString *, NSString *> *lossy __attribute__((swift_name("lossy")));
@property (readonly) NSString *strike __attribute__((swift_name("strike")));
@end


/**
 * Maps each content format's facet `$type` strings to the markdown marks
 * Inkwell supports, plus human-readable labels for features that can't be
 * represented in markdown.
 *
 * Mirrors iOS `FacetSchema` in ContentProvider.swift and the hard-coded
 * NSID strings duplicated across Android's MarkdownConverter.kt,
 * PcktOffprintConverter.kt, and LeafletBlockRenderer.kt.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FacetSchema")))
@interface InkwellSharedFacetSchema : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Maps each content format's facet `$type` strings to the markdown marks
 * Inkwell supports, plus human-readable labels for features that can't be
 * represented in markdown.
 *
 * Mirrors iOS `FacetSchema` in ContentProvider.swift and the hard-coded
 * NSID strings duplicated across Android's MarkdownConverter.kt,
 * PcktOffprintConverter.kt, and LeafletBlockRenderer.kt.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)facetSchema __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedFacetSchema *shared __attribute__((swift_name("shared")));
@property (readonly) InkwellSharedFacetDefinition *leaflet __attribute__((swift_name("leaflet")));
@property (readonly) InkwellSharedFacetDefinition *offprint __attribute__((swift_name("offprint")));
@property (readonly) InkwellSharedFacetDefinition *pckt __attribute__((swift_name("pckt")));
@end

__attribute__((objc_subclassing_restricted))
@interface RichTextFacet : InkwellSharedBase
- (instancetype)initWithByteStart:(int32_t)byteStart byteEnd:(int32_t)byteEnd features:(NSArray<RichTextFeature *> *)features __attribute__((swift_name("init(byteStart:byteEnd:features:)"))) __attribute__((objc_designated_initializer));
- (RichTextFacet *)doCopyByteStart:(int32_t)byteStart byteEnd:(int32_t)byteEnd features:(NSArray<RichTextFeature *> *)features __attribute__((swift_name("doCopy(byteStart:byteEnd:features:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t byteEnd __attribute__((swift_name("byteEnd")));
@property (readonly) int32_t byteStart __attribute__((swift_name("byteStart")));
@property (readonly) NSArray<RichTextFeature *> *features __attribute__((swift_name("features")));
@end

__attribute__((objc_subclassing_restricted))
@interface RichTextFeature : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type uri:(NSString * _Nullable)uri __attribute__((swift_name("init(type:uri:)"))) __attribute__((objc_designated_initializer));
- (RichTextFeature *)doCopyType:(NSString *)type uri:(NSString * _Nullable)uri __attribute__((swift_name("doCopy(type:uri:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@property (readonly) NSString * _Nullable uri __attribute__((swift_name("uri")));
@end


/**
 * Constants for submitting in-app feedback to userinput.app
 * (https://userinput.app), a federated feedback board built on AT Protocol.
 *
 * Inkwell only ever *creates* `app.userinput.discussion` records in the
 * signed-in user's own repo, pointing at Inkwell's own feedback space
 * (owned by ewancroft.uk) via a strong reference — it doesn't implement
 * the rest of userinput.app's lexicon surface (voting, moderation,
 * replies, etc.), which isn't needed for a "send feedback" flow.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserInputLexicon")))
@interface InkwellSharedUserInputLexicon : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Constants for submitting in-app feedback to userinput.app
 * (https://userinput.app), a federated feedback board built on AT Protocol.
 *
 * Inkwell only ever *creates* `app.userinput.discussion` records in the
 * signed-in user's own repo, pointing at Inkwell's own feedback space
 * (owned by ewancroft.uk) via a strong reference — it doesn't implement
 * the rest of userinput.app's lexicon surface (voting, moderation,
 * replies, etc.), which isn't needed for a "send feedback" flow.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)userInputLexicon __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedUserInputLexicon *shared __attribute__((swift_name("shared")));

/** `app.userinput.discussion.body` — maximum length in Unicode grapheme clusters. */
@property (readonly) int32_t BODY_MAX_GRAPHEMES __attribute__((swift_name("BODY_MAX_GRAPHEMES")));

/** `app.userinput.discussion.body` — maximum length in UTF-16 code units. */
@property (readonly) int32_t BODY_MAX_LENGTH __attribute__((swift_name("BODY_MAX_LENGTH")));

/** A feedback post, created in the submitting user's own repo. */
@property (readonly) NSString *DISCUSSION __attribute__((swift_name("DISCUSSION")));

/** Inkwell's own feedback space, owned by ewancroft.uk. */
@property (readonly) NSString *INKWELL_SPACE_URI __attribute__((swift_name("INKWELL_SPACE_URI")));

/** Maximum number of tags a discussion may carry. */
@property (readonly) int32_t MAX_TAGS __attribute__((swift_name("MAX_TAGS")));

/** A feedback board/space. Inkwell only reads its own (see [INKWELL_SPACE_URI]). */
@property (readonly) NSString *SPACE __attribute__((swift_name("SPACE")));

/** Tag values defined on Inkwell's feedback space, in display order. */
@property (readonly) NSArray<NSString *> *TAGS __attribute__((swift_name("TAGS")));

/** `app.userinput.discussion.title` — maximum length in Unicode grapheme clusters. */
@property (readonly) int32_t TITLE_MAX_GRAPHEMES __attribute__((swift_name("TITLE_MAX_GRAPHEMES")));

/** `app.userinput.discussion.title` — maximum length in UTF-16 code units. */
@property (readonly) int32_t TITLE_MAX_LENGTH __attribute__((swift_name("TITLE_MAX_LENGTH")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CollectionNsids")))
@interface InkwellSharedCollectionNsids : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)collectionNsids __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedCollectionNsids *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *DOCUMENT __attribute__((swift_name("DOCUMENT")));
@property (readonly) NSString *GRAPH_RECOMMEND __attribute__((swift_name("GRAPH_RECOMMEND")));
@property (readonly) NSString *GRAPH_SUBSCRIPTION __attribute__((swift_name("GRAPH_SUBSCRIPTION")));
@property (readonly) NSString *LEAFLET_COMMENT __attribute__((swift_name("LEAFLET_COMMENT")));
@property (readonly) NSString *LEAFLET_POLL_DEFINITION __attribute__((swift_name("LEAFLET_POLL_DEFINITION")));
@property (readonly) NSString *LEAFLET_POLL_VOTE __attribute__((swift_name("LEAFLET_POLL_VOTE")));
@property (readonly) NSString *PUBLICATION __attribute__((swift_name("PUBLICATION")));
@end


/**
 * The Privacy Policy and Terms of Service, as markdown, rendered natively
 * in-app on both iOS and Android by the same shared markdown renderer used
 * for reading Standard.site content -- see MarkdownRendererView on each
 * platform. iOS consumes this via SharedKMP.swift; Android consumes it
 * directly. The website renders its own HTML copy from the same source,
 * generated separately since it isn't part of this KMP module.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LegalDocuments")))
@interface InkwellSharedLegalDocuments : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * The Privacy Policy and Terms of Service, as markdown, rendered natively
 * in-app on both iOS and Android by the same shared markdown renderer used
 * for reading Standard.site content -- see MarkdownRendererView on each
 * platform. iOS consumes this via SharedKMP.swift; Android consumes it
 * directly. The website renders its own HTML copy from the same source,
 * generated separately since it isn't part of this KMP module.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)legalDocuments __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedLegalDocuments *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *EFFECTIVE_DATE __attribute__((swift_name("EFFECTIVE_DATE")));
@property (readonly) NSString *EFFECTIVE_DATE_ISO __attribute__((swift_name("EFFECTIVE_DATE_ISO")));
@property (readonly) NSString *VERSION __attribute__((swift_name("VERSION")));
@property (readonly) NSString *privacyMarkdown __attribute__((swift_name("privacyMarkdown")));
@property (readonly) NSString *termsMarkdown __attribute__((swift_name("termsMarkdown")));
@end


/**
 * Platform-agnostic inline markdown scanner.
 *
 * Parses inline markdown syntax (**bold**, *italic*, `code`, ~~strike~~,
 * [text](url)) into a list of [InlineSegment] items. Each platform maps
 * these to its own attributed-string type (SwiftUI AttributedString,
 * Compose AnnotatedString).
 *
 * The scanner strips markdown delimiters from the output text — segments
 * contain the visible text only. Byte offsets are not tracked here; the
 * caller already has the plaintext and can compute offsets if needed.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InlineMarkdownScanner")))
@interface InkwellSharedInlineMarkdownScanner : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Platform-agnostic inline markdown scanner.
 *
 * Parses inline markdown syntax (**bold**, *italic*, `code`, ~~strike~~,
 * [text](url)) into a list of [InlineSegment] items. Each platform maps
 * these to its own attributed-string type (SwiftUI AttributedString,
 * Compose AnnotatedString).
 *
 * The scanner strips markdown delimiters from the output text — segments
 * contain the visible text only. Byte offsets are not tracked here; the
 * caller already has the plaintext and can compute offsets if needed.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)inlineMarkdownScanner __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedInlineMarkdownScanner *shared __attribute__((swift_name("shared")));

/**
 * Parses [text] for inline markdown and returns the list of segments.
 * Unmatched delimiters are treated as literal text.
 */
- (NSArray<InkwellSharedInlineSegment *> *)scanText:(NSString *)text __attribute__((swift_name("scan(text:)")));
@end


/**
 * A single segment of inline-formatted text.
 */
__attribute__((swift_name("InlineSegment")))
@interface InkwellSharedInlineSegment : InkwellSharedBase
@end


/** Bold text (was wrapped in `**`). */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InlineSegment.Bold")))
@interface InkwellSharedInlineSegmentBold : InkwellSharedInlineSegment
- (instancetype)initWithText:(NSString *)text __attribute__((swift_name("init(text:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedInlineSegmentBold *)doCopyText:(NSString *)text __attribute__((swift_name("doCopy(text:)")));

/** Bold text (was wrapped in `**`). */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/** Bold text (was wrapped in `**`). */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/** Bold text (was wrapped in `**`). */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end


/** Inline code (was wrapped in `` ` ``). */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InlineSegment.Code")))
@interface InkwellSharedInlineSegmentCode : InkwellSharedInlineSegment
- (instancetype)initWithText:(NSString *)text __attribute__((swift_name("init(text:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedInlineSegmentCode *)doCopyText:(NSString *)text __attribute__((swift_name("doCopy(text:)")));

/** Inline code (was wrapped in `` ` ``). */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/** Inline code (was wrapped in `` ` ``). */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/** Inline code (was wrapped in `` ` ``). */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end


/** Italic text (was wrapped in `*`). */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InlineSegment.Italic")))
@interface InkwellSharedInlineSegmentItalic : InkwellSharedInlineSegment
- (instancetype)initWithText:(NSString *)text __attribute__((swift_name("init(text:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedInlineSegmentItalic *)doCopyText:(NSString *)text __attribute__((swift_name("doCopy(text:)")));

/** Italic text (was wrapped in `*`). */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/** Italic text (was wrapped in `*`). */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/** Italic text (was wrapped in `*`). */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end


/** A hyperlink. [text] is the visible label, [url] is the destination. */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InlineSegment.Link")))
@interface InkwellSharedInlineSegmentLink : InkwellSharedInlineSegment
- (instancetype)initWithText:(NSString *)text url:(NSString *)url __attribute__((swift_name("init(text:url:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedInlineSegmentLink *)doCopyText:(NSString *)text url:(NSString *)url __attribute__((swift_name("doCopy(text:url:)")));

/** A hyperlink. [text] is the visible label, [url] is the destination. */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/** A hyperlink. [text] is the visible label, [url] is the destination. */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/** A hyperlink. [text] is the visible label, [url] is the destination. */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@property (readonly) NSString *url __attribute__((swift_name("url")));
@end


/** Plain text with no formatting. */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InlineSegment.Plain")))
@interface InkwellSharedInlineSegmentPlain : InkwellSharedInlineSegment
- (instancetype)initWithText:(NSString *)text __attribute__((swift_name("init(text:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedInlineSegmentPlain *)doCopyText:(NSString *)text __attribute__((swift_name("doCopy(text:)")));

/** Plain text with no formatting. */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/** Plain text with no formatting. */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/** Plain text with no formatting. */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end


/** Strikethrough text (was wrapped in `~~`). */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InlineSegment.Strike")))
@interface InkwellSharedInlineSegmentStrike : InkwellSharedInlineSegment
- (instancetype)initWithText:(NSString *)text __attribute__((swift_name("init(text:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedInlineSegmentStrike *)doCopyText:(NSString *)text __attribute__((swift_name("doCopy(text:)")));

/** Strikethrough text (was wrapped in `~~`). */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/** Strikethrough text (was wrapped in `~~`). */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/** Strikethrough text (was wrapped in `~~`). */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end


/**
 * A markdown block produced by [MarkdownParser.parse].
 *
 * Mirrors iOS `MarkdownBlock` enum in ContentProvider.swift and Android
 * `MarkdownBlock` sealed class in MarkdownParser.kt.
 */
__attribute__((swift_name("MarkdownBlock")))
@interface InkwellSharedMarkdownBlock : InkwellSharedBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownBlock.Blockquote")))
@interface InkwellSharedMarkdownBlockBlockquote : InkwellSharedMarkdownBlock
- (instancetype)initWithText:(NSString *)text __attribute__((swift_name("init(text:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedMarkdownBlockBlockquote *)doCopyText:(NSString *)text __attribute__((swift_name("doCopy(text:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownBlock.Code")))
@interface InkwellSharedMarkdownBlockCode : InkwellSharedMarkdownBlock
- (instancetype)initWithLanguage:(NSString * _Nullable)language content:(NSString *)content __attribute__((swift_name("init(language:content:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedMarkdownBlockCode *)doCopyLanguage:(NSString * _Nullable)language content:(NSString *)content __attribute__((swift_name("doCopy(language:content:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *content __attribute__((swift_name("content")));
@property (readonly) NSString * _Nullable language __attribute__((swift_name("language")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownBlock.Heading")))
@interface InkwellSharedMarkdownBlockHeading : InkwellSharedMarkdownBlock
- (instancetype)initWithLevel:(int32_t)level text:(NSString *)text __attribute__((swift_name("init(level:text:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedMarkdownBlockHeading *)doCopyLevel:(int32_t)level text:(NSString *)text __attribute__((swift_name("doCopy(level:text:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t level __attribute__((swift_name("level")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownBlock.HorizontalRule")))
@interface InkwellSharedMarkdownBlockHorizontalRule : InkwellSharedMarkdownBlock
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)horizontalRule __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedMarkdownBlockHorizontalRule *shared __attribute__((swift_name("shared")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownBlock.Image")))
@interface InkwellSharedMarkdownBlockImage : InkwellSharedMarkdownBlock
- (instancetype)initWithAlt:(NSString *)alt url:(NSString *)url __attribute__((swift_name("init(alt:url:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedMarkdownBlockImage *)doCopyAlt:(NSString *)alt url:(NSString *)url __attribute__((swift_name("doCopy(alt:url:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *alt __attribute__((swift_name("alt")));
@property (readonly) NSString *url __attribute__((swift_name("url")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownBlock.Math")))
@interface InkwellSharedMarkdownBlockMath : InkwellSharedMarkdownBlock
- (instancetype)initWithTex:(NSString *)tex __attribute__((swift_name("init(tex:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedMarkdownBlockMath *)doCopyTex:(NSString *)tex __attribute__((swift_name("doCopy(tex:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *tex __attribute__((swift_name("tex")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownBlock.OrderedList")))
@interface InkwellSharedMarkdownBlockOrderedList : InkwellSharedMarkdownBlock
- (instancetype)initWithStart:(int32_t)start items:(NSArray<InkwellSharedMarkdownListItem *> *)items __attribute__((swift_name("init(start:items:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedMarkdownBlockOrderedList *)doCopyStart:(int32_t)start items:(NSArray<InkwellSharedMarkdownListItem *> *)items __attribute__((swift_name("doCopy(start:items:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<InkwellSharedMarkdownListItem *> *items __attribute__((swift_name("items")));
@property (readonly) int32_t start __attribute__((swift_name("start")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownBlock.Paragraph")))
@interface InkwellSharedMarkdownBlockParagraph : InkwellSharedMarkdownBlock
- (instancetype)initWithText:(NSString *)text __attribute__((swift_name("init(text:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedMarkdownBlockParagraph *)doCopyText:(NSString *)text __attribute__((swift_name("doCopy(text:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownBlock.TaskList")))
@interface InkwellSharedMarkdownBlockTaskList : InkwellSharedMarkdownBlock
- (instancetype)initWithItems:(NSArray<InkwellSharedMarkdownListItem *> *)items __attribute__((swift_name("init(items:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedMarkdownBlockTaskList *)doCopyItems:(NSArray<InkwellSharedMarkdownListItem *> *)items __attribute__((swift_name("doCopy(items:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<InkwellSharedMarkdownListItem *> *items __attribute__((swift_name("items")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownBlock.UnorderedList")))
@interface InkwellSharedMarkdownBlockUnorderedList : InkwellSharedMarkdownBlock
- (instancetype)initWithItems:(NSArray<InkwellSharedMarkdownListItem *> *)items __attribute__((swift_name("init(items:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedMarkdownBlockUnorderedList *)doCopyItems:(NSArray<InkwellSharedMarkdownListItem *> *)items __attribute__((swift_name("doCopy(items:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<InkwellSharedMarkdownListItem *> *items __attribute__((swift_name("items")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownListItem")))
@interface InkwellSharedMarkdownListItem : InkwellSharedBase
- (instancetype)initWithText:(NSString *)text checked:(InkwellSharedBoolean * _Nullable)checked children:(NSArray<InkwellSharedMarkdownListItem *> * _Nullable)children __attribute__((swift_name("init(text:checked:children:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedMarkdownListItem *)doCopyText:(NSString *)text checked:(InkwellSharedBoolean * _Nullable)checked children:(NSArray<InkwellSharedMarkdownListItem *> * _Nullable)children __attribute__((swift_name("doCopy(text:checked:children:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) InkwellSharedBoolean * _Nullable checked __attribute__((swift_name("checked")));
@property (readonly) NSArray<InkwellSharedMarkdownListItem *> * _Nullable children __attribute__((swift_name("children")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end


/**
 * Line-by-line markdown parser handling the block types common to all
 * standard.site providers. Not a full CommonMark parser, but sufficient
 * for the reader's rendering and the writer's round-trip needs.
 *
 * Mirrors iOS `MarkdownParser` in ContentProvider.swift and Android
 * `MarkdownParser` in MarkdownParser.kt — near-identical logic.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownParser")))
@interface InkwellSharedMarkdownParser : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Line-by-line markdown parser handling the block types common to all
 * standard.site providers. Not a full CommonMark parser, but sufficient
 * for the reader's rendering and the writer's round-trip needs.
 *
 * Mirrors iOS `MarkdownParser` in ContentProvider.swift and Android
 * `MarkdownParser` in MarkdownParser.kt — near-identical logic.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)markdownParser __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedMarkdownParser *shared __attribute__((swift_name("shared")));
- (NSArray<InkwellSharedMarkdownBlock *> *)parseMarkdown:(NSString *)markdown __attribute__((swift_name("parse(markdown:)")));
@end


/**
 * Converts [MarkdownBlock] arrays back to markdown strings.
 *
 * Mirrors iOS `MarkdownSerializer` in ContentProvider.swift. Android's
 * `MarkdownConverter` inlines this logic per format; the shared serializer
 * provides a single canonical round-trip.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarkdownSerializer")))
@interface InkwellSharedMarkdownSerializer : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Converts [MarkdownBlock] arrays back to markdown strings.
 *
 * Mirrors iOS `MarkdownSerializer` in ContentProvider.swift. Android's
 * `MarkdownConverter` inlines this logic per format; the shared serializer
 * provides a single canonical round-trip.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)markdownSerializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedMarkdownSerializer *shared __attribute__((swift_name("shared")));
- (NSString *)serializeBlocks:(NSArray<InkwellSharedMarkdownBlock *> *)blocks __attribute__((swift_name("serialize(blocks:)")));
@end


/**
 * Neutral shared model for a publication-level basic theme.
 *
 * Four-colour palette: background, foreground, accent, accentForeground.
 * Maps to Android `BasicTheme` and iOS `BasicDefinition`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BasicTheme")))
@interface InkwellSharedBasicTheme : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type background:(InkwellSharedRgbColor *)background foreground:(InkwellSharedRgbColor *)foreground accent:(InkwellSharedRgbColor *)accent accentForeground:(InkwellSharedRgbColor *)accentForeground __attribute__((swift_name("init(type:background:foreground:accent:accentForeground:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedBasicTheme *)doCopyType:(NSString *)type background:(InkwellSharedRgbColor *)background foreground:(InkwellSharedRgbColor *)foreground accent:(InkwellSharedRgbColor *)accent accentForeground:(InkwellSharedRgbColor *)accentForeground __attribute__((swift_name("doCopy(type:background:foreground:accent:accentForeground:)")));

/**
 * Neutral shared model for a publication-level basic theme.
 *
 * Four-colour palette: background, foreground, accent, accentForeground.
 * Maps to Android `BasicTheme` and iOS `BasicDefinition`.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a publication-level basic theme.
 *
 * Four-colour palette: background, foreground, accent, accentForeground.
 * Maps to Android `BasicTheme` and iOS `BasicDefinition`.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a publication-level basic theme.
 *
 * Four-colour palette: background, foreground, accent, accentForeground.
 * Maps to Android `BasicTheme` and iOS `BasicDefinition`.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) InkwellSharedRgbColor *accent __attribute__((swift_name("accent")));
@property (readonly) InkwellSharedRgbColor *accentForeground __attribute__((swift_name("accentForeground")));
@property (readonly) InkwellSharedRgbColor *background __attribute__((swift_name("background")));
@property (readonly) InkwellSharedRgbColor *foreground __attribute__((swift_name("foreground")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end


/**
 * Neutral shared model for an AT Protocol blob reference.
 *
 * Mirrors Android `BlobRef` and iOS `ComAtprotoLexicon.Repository.UploadBlobOutput`.
 * The `link` field corresponds to the `$link` key in AT Protocol JSON.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlobRef")))
@interface InkwellSharedBlobRef : InkwellSharedBase
- (instancetype)initWithLink:(NSString *)link size:(int32_t)size type:(NSString *)type mimeType:(NSString * _Nullable)mimeType __attribute__((swift_name("init(link:size:type:mimeType:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedBlobRef *)doCopyLink:(NSString *)link size:(int32_t)size type:(NSString *)type mimeType:(NSString * _Nullable)mimeType __attribute__((swift_name("doCopy(link:size:type:mimeType:)")));

/**
 * Neutral shared model for an AT Protocol blob reference.
 *
 * Mirrors Android `BlobRef` and iOS `ComAtprotoLexicon.Repository.UploadBlobOutput`.
 * The `link` field corresponds to the `$link` key in AT Protocol JSON.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for an AT Protocol blob reference.
 *
 * Mirrors Android `BlobRef` and iOS `ComAtprotoLexicon.Repository.UploadBlobOutput`.
 * The `link` field corresponds to the `$link` key in AT Protocol JSON.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for an AT Protocol blob reference.
 *
 * Mirrors Android `BlobRef` and iOS `ComAtprotoLexicon.Repository.UploadBlobOutput`.
 * The `link` field corresponds to the `$link` key in AT Protocol JSON.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *link __attribute__((swift_name("link")));
@property (readonly) NSString * _Nullable mimeType __attribute__((swift_name("mimeType")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end


/**
 * Neutral shared model for a Leaflet byte slice (UTF-8 offset range).
 *
 * Mirrors Android `ByteSlice` and iOS `LeafletByteSlice`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ByteSlice")))
@interface InkwellSharedByteSlice : InkwellSharedBase
- (instancetype)initWithByteStart:(int32_t)byteStart byteEnd:(int32_t)byteEnd __attribute__((swift_name("init(byteStart:byteEnd:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedByteSlice *)doCopyByteStart:(int32_t)byteStart byteEnd:(int32_t)byteEnd __attribute__((swift_name("doCopy(byteStart:byteEnd:)")));

/**
 * Neutral shared model for a Leaflet byte slice (UTF-8 offset range).
 *
 * Mirrors Android `ByteSlice` and iOS `LeafletByteSlice`.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a Leaflet byte slice (UTF-8 offset range).
 *
 * Mirrors Android `ByteSlice` and iOS `LeafletByteSlice`.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a Leaflet byte slice (UTF-8 offset range).
 *
 * Mirrors Android `ByteSlice` and iOS `LeafletByteSlice`.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t byteEnd __attribute__((swift_name("byteEnd")));
@property (readonly) int32_t byteStart __attribute__((swift_name("byteStart")));
@end


/**
 * Neutral shared model for a Leaflet rich theme colour value.
 *
 * Supports both RGB and RGBA via the optional alpha channel.
 * Alpha is a percentage 0-100, defaulting to 100 (opaque).
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ColorValue")))
@interface InkwellSharedColorValue : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type r:(int32_t)r g:(int32_t)g b:(int32_t)b a:(InkwellSharedInt * _Nullable)a __attribute__((swift_name("init(type:r:g:b:a:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedColorValue *)doCopyType:(NSString *)type r:(int32_t)r g:(int32_t)g b:(int32_t)b a:(InkwellSharedInt * _Nullable)a __attribute__((swift_name("doCopy(type:r:g:b:a:)")));

/**
 * Neutral shared model for a Leaflet rich theme colour value.
 *
 * Supports both RGB and RGBA via the optional alpha channel.
 * Alpha is a percentage 0-100, defaulting to 100 (opaque).
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a Leaflet rich theme colour value.
 *
 * Supports both RGB and RGBA via the optional alpha channel.
 * Alpha is a percentage 0-100, defaulting to 100 (opaque).
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a Leaflet rich theme colour value.
 *
 * Supports both RGB and RGBA via the optional alpha channel.
 * Alpha is a percentage 0-100, defaulting to 100 (opaque).
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) InkwellSharedInt * _Nullable a __attribute__((swift_name("a")));
@property (readonly) int32_t b __attribute__((swift_name("b")));
@property (readonly) int32_t g __attribute__((swift_name("g")));
@property (readonly) int32_t r __attribute__((swift_name("r")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end


/**
 * Neutral shared model for per-document display preferences.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DocumentPreferences")))
@interface InkwellSharedDocumentPreferences : InkwellSharedBase
- (instancetype)initWithShowComments:(InkwellSharedBoolean * _Nullable)showComments showMentions:(InkwellSharedBoolean * _Nullable)showMentions showRecommends:(InkwellSharedBoolean * _Nullable)showRecommends showPrevNext:(InkwellSharedBoolean * _Nullable)showPrevNext showInDiscover:(InkwellSharedBoolean * _Nullable)showInDiscover __attribute__((swift_name("init(showComments:showMentions:showRecommends:showPrevNext:showInDiscover:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedDocumentPreferences *)doCopyShowComments:(InkwellSharedBoolean * _Nullable)showComments showMentions:(InkwellSharedBoolean * _Nullable)showMentions showRecommends:(InkwellSharedBoolean * _Nullable)showRecommends showPrevNext:(InkwellSharedBoolean * _Nullable)showPrevNext showInDiscover:(InkwellSharedBoolean * _Nullable)showInDiscover __attribute__((swift_name("doCopy(showComments:showMentions:showRecommends:showPrevNext:showInDiscover:)")));

/**
 * Neutral shared model for per-document display preferences.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for per-document display preferences.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for per-document display preferences.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) InkwellSharedBoolean * _Nullable showComments __attribute__((swift_name("showComments")));
@property (readonly) InkwellSharedBoolean * _Nullable showInDiscover __attribute__((swift_name("showInDiscover")));
@property (readonly) InkwellSharedBoolean * _Nullable showMentions __attribute__((swift_name("showMentions")));
@property (readonly) InkwellSharedBoolean * _Nullable showPrevNext __attribute__((swift_name("showPrevNext")));
@property (readonly) InkwellSharedBoolean * _Nullable showRecommends __attribute__((swift_name("showRecommends")));
@end


/**
 * Neutral shared model for a Leaflet inline facet (byte-range formatting).
 *
 * Mirrors Android `LeafletFacet` and iOS `LeafletFacet`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LeafletFacet")))
@interface InkwellSharedLeafletFacet : InkwellSharedBase
- (instancetype)initWithType:(NSString * _Nullable)type index:(InkwellSharedByteSlice *)index features:(NSArray<InkwellSharedLeafletFacetFeature *> *)features __attribute__((swift_name("init(type:index:features:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedLeafletFacet *)doCopyType:(NSString * _Nullable)type index:(InkwellSharedByteSlice *)index features:(NSArray<InkwellSharedLeafletFacetFeature *> *)features __attribute__((swift_name("doCopy(type:index:features:)")));

/**
 * Neutral shared model for a Leaflet inline facet (byte-range formatting).
 *
 * Mirrors Android `LeafletFacet` and iOS `LeafletFacet`.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a Leaflet inline facet (byte-range formatting).
 *
 * Mirrors Android `LeafletFacet` and iOS `LeafletFacet`.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a Leaflet inline facet (byte-range formatting).
 *
 * Mirrors Android `LeafletFacet` and iOS `LeafletFacet`.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<InkwellSharedLeafletFacetFeature *> *features __attribute__((swift_name("features")));
@property (readonly) InkwellSharedByteSlice *index __attribute__((swift_name("index")));
@property (readonly) NSString * _Nullable type __attribute__((swift_name("type")));
@end


/**
 * Neutral shared model for a Leaflet facet feature.
 *
 * Mirrors Android `FacetFeature` and iOS `LeafletFacetFeature`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LeafletFacetFeature")))
@interface InkwellSharedLeafletFacetFeature : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type uri:(NSString * _Nullable)uri tag:(NSString * _Nullable)tag did:(NSString * _Nullable)did __attribute__((swift_name("init(type:uri:tag:did:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedLeafletFacetFeature *)doCopyType:(NSString *)type uri:(NSString * _Nullable)uri tag:(NSString * _Nullable)tag did:(NSString * _Nullable)did __attribute__((swift_name("doCopy(type:uri:tag:did:)")));

/**
 * Neutral shared model for a Leaflet facet feature.
 *
 * Mirrors Android `FacetFeature` and iOS `LeafletFacetFeature`.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a Leaflet facet feature.
 *
 * Mirrors Android `FacetFeature` and iOS `LeafletFacetFeature`.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a Leaflet facet feature.
 *
 * Mirrors Android `FacetFeature` and iOS `LeafletFacetFeature`.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable did __attribute__((swift_name("did")));
@property (readonly) NSString * _Nullable tag __attribute__((swift_name("tag")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@property (readonly) NSString * _Nullable uri __attribute__((swift_name("uri")));
@end


/**
 * Neutral shared model for a legacy light/dark palette.
 *
 * Older standard.site applications emit this shape instead of the full
 * Leaflet rich theme. Contains hex colour strings for five UI elements.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LegacyPalette")))
@interface InkwellSharedLegacyPalette : InkwellSharedBase
- (instancetype)initWithBackground:(NSString * _Nullable)background text:(NSString * _Nullable)text accent:(NSString * _Nullable)accent link:(NSString * _Nullable)link surfaceHover:(NSString * _Nullable)surfaceHover __attribute__((swift_name("init(background:text:accent:link:surfaceHover:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedLegacyPalette *)doCopyBackground:(NSString * _Nullable)background text:(NSString * _Nullable)text accent:(NSString * _Nullable)accent link:(NSString * _Nullable)link surfaceHover:(NSString * _Nullable)surfaceHover __attribute__((swift_name("doCopy(background:text:accent:link:surfaceHover:)")));

/**
 * Neutral shared model for a legacy light/dark palette.
 *
 * Older standard.site applications emit this shape instead of the full
 * Leaflet rich theme. Contains hex colour strings for five UI elements.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a legacy light/dark palette.
 *
 * Older standard.site applications emit this shape instead of the full
 * Leaflet rich theme. Contains hex colour strings for five UI elements.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a legacy light/dark palette.
 *
 * Older standard.site applications emit this shape instead of the full
 * Leaflet rich theme. Contains hex colour strings for five UI elements.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable accent __attribute__((swift_name("accent")));
@property (readonly) NSString * _Nullable background __attribute__((swift_name("background")));
@property (readonly) NSString * _Nullable link __attribute__((swift_name("link")));
@property (readonly) NSString * _Nullable surfaceHover __attribute__((swift_name("surfaceHover")));
@property (readonly) NSString * _Nullable text __attribute__((swift_name("text")));
@end


/**
 * Neutral shared model for publication-level display preferences.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PublicationPreferences")))
@interface InkwellSharedPublicationPreferences : InkwellSharedBase
- (instancetype)initWithShowInDiscover:(InkwellSharedBoolean * _Nullable)showInDiscover __attribute__((swift_name("init(showInDiscover:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedPublicationPreferences *)doCopyShowInDiscover:(InkwellSharedBoolean * _Nullable)showInDiscover __attribute__((swift_name("doCopy(showInDiscover:)")));

/**
 * Neutral shared model for publication-level display preferences.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for publication-level display preferences.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for publication-level display preferences.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) InkwellSharedBoolean * _Nullable showInDiscover __attribute__((swift_name("showInDiscover")));
@end


/**
 * Neutral shared model for a rich Leaflet publication theme.
 *
 * Mirrors the structure of Android `PublicationTheme` and iOS
 * `SiteStandardLexicon.Theme.PublicationTheme`. Supports the older
 * light/dark palette shape via [light] and [dark].
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PublicationTheme")))
@interface InkwellSharedPublicationTheme : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type backgroundColor:(InkwellSharedColorValue * _Nullable)backgroundColor pageBackground:(InkwellSharedColorValue * _Nullable)pageBackground primary:(InkwellSharedColorValue * _Nullable)primary accentBackground:(InkwellSharedColorValue * _Nullable)accentBackground accentText:(InkwellSharedColorValue * _Nullable)accentText pageWidth:(InkwellSharedInt * _Nullable)pageWidth showPageBackground:(InkwellSharedBoolean * _Nullable)showPageBackground headingFont:(NSString * _Nullable)headingFont bodyFont:(NSString * _Nullable)bodyFont font:(NSString * _Nullable)font light:(InkwellSharedLegacyPalette * _Nullable)light dark:(InkwellSharedLegacyPalette * _Nullable)dark __attribute__((swift_name("init(type:backgroundColor:pageBackground:primary:accentBackground:accentText:pageWidth:showPageBackground:headingFont:bodyFont:font:light:dark:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedPublicationTheme *)doCopyType:(NSString *)type backgroundColor:(InkwellSharedColorValue * _Nullable)backgroundColor pageBackground:(InkwellSharedColorValue * _Nullable)pageBackground primary:(InkwellSharedColorValue * _Nullable)primary accentBackground:(InkwellSharedColorValue * _Nullable)accentBackground accentText:(InkwellSharedColorValue * _Nullable)accentText pageWidth:(InkwellSharedInt * _Nullable)pageWidth showPageBackground:(InkwellSharedBoolean * _Nullable)showPageBackground headingFont:(NSString * _Nullable)headingFont bodyFont:(NSString * _Nullable)bodyFont font:(NSString * _Nullable)font light:(InkwellSharedLegacyPalette * _Nullable)light dark:(InkwellSharedLegacyPalette * _Nullable)dark __attribute__((swift_name("doCopy(type:backgroundColor:pageBackground:primary:accentBackground:accentText:pageWidth:showPageBackground:headingFont:bodyFont:font:light:dark:)")));

/**
 * Neutral shared model for a rich Leaflet publication theme.
 *
 * Mirrors the structure of Android `PublicationTheme` and iOS
 * `SiteStandardLexicon.Theme.PublicationTheme`. Supports the older
 * light/dark palette shape via [light] and [dark].
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a rich Leaflet publication theme.
 *
 * Mirrors the structure of Android `PublicationTheme` and iOS
 * `SiteStandardLexicon.Theme.PublicationTheme`. Supports the older
 * light/dark palette shape via [light] and [dark].
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a rich Leaflet publication theme.
 *
 * Mirrors the structure of Android `PublicationTheme` and iOS
 * `SiteStandardLexicon.Theme.PublicationTheme`. Supports the older
 * light/dark palette shape via [light] and [dark].
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) InkwellSharedColorValue * _Nullable accentBackground __attribute__((swift_name("accentBackground")));
@property (readonly) InkwellSharedColorValue * _Nullable accentText __attribute__((swift_name("accentText")));
@property (readonly) InkwellSharedColorValue * _Nullable backgroundColor __attribute__((swift_name("backgroundColor")));
@property (readonly) NSString * _Nullable bodyFont __attribute__((swift_name("bodyFont")));
@property (readonly) InkwellSharedLegacyPalette * _Nullable dark __attribute__((swift_name("dark")));
@property (readonly) NSString * _Nullable font __attribute__((swift_name("font")));
@property (readonly) NSString * _Nullable headingFont __attribute__((swift_name("headingFont")));
@property (readonly) InkwellSharedLegacyPalette * _Nullable light __attribute__((swift_name("light")));
@property (readonly) InkwellSharedColorValue * _Nullable pageBackground __attribute__((swift_name("pageBackground")));
@property (readonly) InkwellSharedInt * _Nullable pageWidth __attribute__((swift_name("pageWidth")));
@property (readonly) InkwellSharedColorValue * _Nullable primary __attribute__((swift_name("primary")));
@property (readonly) InkwellSharedBoolean * _Nullable showPageBackground __attribute__((swift_name("showPageBackground")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end


/**
 * Neutral shared model for an opaque RGB colour.
 *
 * Used by theme resolution across both platforms. The native platform
 * types map to/from this when crossing the KMP boundary.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RgbColor")))
@interface InkwellSharedRgbColor : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type r:(int32_t)r g:(int32_t)g b:(int32_t)b __attribute__((swift_name("init(type:r:g:b:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedRgbColor *)doCopyType:(NSString *)type r:(int32_t)r g:(int32_t)g b:(int32_t)b __attribute__((swift_name("doCopy(type:r:g:b:)")));

/**
 * Neutral shared model for an opaque RGB colour.
 *
 * Used by theme resolution across both platforms. The native platform
 * types map to/from this when crossing the KMP boundary.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for an opaque RGB colour.
 *
 * Used by theme resolution across both platforms. The native platform
 * types map to/from this when crossing the KMP boundary.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for an opaque RGB colour.
 *
 * Used by theme resolution across both platforms. The native platform
 * types map to/from this when crossing the KMP boundary.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t b __attribute__((swift_name("b")));
@property (readonly) int32_t g __attribute__((swift_name("g")));
@property (readonly) int32_t r __attribute__((swift_name("r")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end


/**
 * Neutral shared model for a translucent RGBA colour.
 *
 * Alpha is stored as a percentage 0-100 (100 = fully opaque), matching
 * both the Android and iOS convention.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RgbaColor")))
@interface InkwellSharedRgbaColor : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type r:(int32_t)r g:(int32_t)g b:(int32_t)b a:(int32_t)a __attribute__((swift_name("init(type:r:g:b:a:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedRgbaColor *)doCopyType:(NSString *)type r:(int32_t)r g:(int32_t)g b:(int32_t)b a:(int32_t)a __attribute__((swift_name("doCopy(type:r:g:b:a:)")));

/**
 * Neutral shared model for a translucent RGBA colour.
 *
 * Alpha is stored as a percentage 0-100 (100 = fully opaque), matching
 * both the Android and iOS convention.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a translucent RGBA colour.
 *
 * Alpha is stored as a percentage 0-100 (100 = fully opaque), matching
 * both the Android and iOS convention.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a translucent RGBA colour.
 *
 * Alpha is stored as a percentage 0-100 (100 = fully opaque), matching
 * both the Android and iOS convention.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t a __attribute__((swift_name("a")));
@property (readonly) int32_t b __attribute__((swift_name("b")));
@property (readonly) int32_t g __attribute__((swift_name("g")));
@property (readonly) int32_t r __attribute__((swift_name("r")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end


/**
 * Neutral shared model for a `site.standard.document` record.
 *
 * Mirrors Android `DocumentRecord` and iOS `DocumentRecord`.
 * Only the fields needed by shared KMP logic and cross-platform mapping
 * are included here.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedDocumentRecord")))
@interface InkwellSharedSharedDocumentRecord : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type site:(NSString *)site title:(NSString *)title publishedAt:(NSString *)publishedAt path:(NSString * _Nullable)path description:(NSString * _Nullable)description tags:(NSArray<NSString *> * _Nullable)tags textContent:(NSString * _Nullable)textContent coverImage:(InkwellSharedBlobRef * _Nullable)coverImage theme:(InkwellSharedPublicationTheme * _Nullable)theme preferences:(InkwellSharedDocumentPreferences * _Nullable)preferences bskyPostRef:(InkwellSharedStrongRef * _Nullable)bskyPostRef __attribute__((swift_name("init(type:site:title:publishedAt:path:description:tags:textContent:coverImage:theme:preferences:bskyPostRef:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedSharedDocumentRecord *)doCopyType:(NSString *)type site:(NSString *)site title:(NSString *)title publishedAt:(NSString *)publishedAt path:(NSString * _Nullable)path description:(NSString * _Nullable)description tags:(NSArray<NSString *> * _Nullable)tags textContent:(NSString * _Nullable)textContent coverImage:(InkwellSharedBlobRef * _Nullable)coverImage theme:(InkwellSharedPublicationTheme * _Nullable)theme preferences:(InkwellSharedDocumentPreferences * _Nullable)preferences bskyPostRef:(InkwellSharedStrongRef * _Nullable)bskyPostRef __attribute__((swift_name("doCopy(type:site:title:publishedAt:path:description:tags:textContent:coverImage:theme:preferences:bskyPostRef:)")));

/**
 * Neutral shared model for a `site.standard.document` record.
 *
 * Mirrors Android `DocumentRecord` and iOS `DocumentRecord`.
 * Only the fields needed by shared KMP logic and cross-platform mapping
 * are included here.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a `site.standard.document` record.
 *
 * Mirrors Android `DocumentRecord` and iOS `DocumentRecord`.
 * Only the fields needed by shared KMP logic and cross-platform mapping
 * are included here.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a `site.standard.document` record.
 *
 * Mirrors Android `DocumentRecord` and iOS `DocumentRecord`.
 * Only the fields needed by shared KMP logic and cross-platform mapping
 * are included here.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) InkwellSharedStrongRef * _Nullable bskyPostRef __attribute__((swift_name("bskyPostRef")));
@property (readonly) InkwellSharedBlobRef * _Nullable coverImage __attribute__((swift_name("coverImage")));
@property (readonly) NSString * _Nullable description_ __attribute__((swift_name("description_")));
@property (readonly) NSString * _Nullable path __attribute__((swift_name("path")));
@property (readonly) InkwellSharedDocumentPreferences * _Nullable preferences __attribute__((swift_name("preferences")));
@property (readonly) NSString *publishedAt __attribute__((swift_name("publishedAt")));
@property (readonly) NSString *site __attribute__((swift_name("site")));
@property (readonly) NSArray<NSString *> * _Nullable tags __attribute__((swift_name("tags")));
@property (readonly) NSString * _Nullable textContent __attribute__((swift_name("textContent")));
@property (readonly) InkwellSharedPublicationTheme * _Nullable theme __attribute__((swift_name("theme")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end


/**
 * Neutral shared model for a `site.standard.graph.recommend` record.
 *
 * Mirrors Android `GraphRecommend` and iOS `Graph.RecommendRecord`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedGraphRecommend")))
@interface InkwellSharedSharedGraphRecommend : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type document:(NSString *)document createdAt:(NSString * _Nullable)createdAt __attribute__((swift_name("init(type:document:createdAt:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedSharedGraphRecommend *)doCopyType:(NSString *)type document:(NSString *)document createdAt:(NSString * _Nullable)createdAt __attribute__((swift_name("doCopy(type:document:createdAt:)")));

/**
 * Neutral shared model for a `site.standard.graph.recommend` record.
 *
 * Mirrors Android `GraphRecommend` and iOS `Graph.RecommendRecord`.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a `site.standard.graph.recommend` record.
 *
 * Mirrors Android `GraphRecommend` and iOS `Graph.RecommendRecord`.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a `site.standard.graph.recommend` record.
 *
 * Mirrors Android `GraphRecommend` and iOS `Graph.RecommendRecord`.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *document __attribute__((swift_name("document")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end


/**
 * Neutral shared model for a `site.standard.graph.subscription` record.
 *
 * Mirrors Android `GraphSubscription` and iOS `Graph.SubscriptionRecord`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedGraphSubscription")))
@interface InkwellSharedSharedGraphSubscription : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type publication:(NSString *)publication createdAt:(NSString * _Nullable)createdAt __attribute__((swift_name("init(type:publication:createdAt:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedSharedGraphSubscription *)doCopyType:(NSString *)type publication:(NSString *)publication createdAt:(NSString * _Nullable)createdAt __attribute__((swift_name("doCopy(type:publication:createdAt:)")));

/**
 * Neutral shared model for a `site.standard.graph.subscription` record.
 *
 * Mirrors Android `GraphSubscription` and iOS `Graph.SubscriptionRecord`.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a `site.standard.graph.subscription` record.
 *
 * Mirrors Android `GraphSubscription` and iOS `Graph.SubscriptionRecord`.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a `site.standard.graph.subscription` record.
 *
 * Mirrors Android `GraphSubscription` and iOS `Graph.SubscriptionRecord`.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *publication __attribute__((swift_name("publication")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end


/**
 * Neutral shared model for a `pub.leaflet.comment` record.
 *
 * Mirrors Android `LeafletComment` and iOS `PubLeafletComment`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedLeafletComment")))
@interface InkwellSharedSharedLeafletComment : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type subject:(NSString *)subject createdAt:(NSString * _Nullable)createdAt plaintext:(NSString *)plaintext facets:(NSArray<InkwellSharedLeafletFacet *> * _Nullable)facets reply:(InkwellSharedSharedLeafletCommentReplyRef * _Nullable)reply onPage:(NSString * _Nullable)onPage __attribute__((swift_name("init(type:subject:createdAt:plaintext:facets:reply:onPage:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedSharedLeafletComment *)doCopyType:(NSString *)type subject:(NSString *)subject createdAt:(NSString * _Nullable)createdAt plaintext:(NSString *)plaintext facets:(NSArray<InkwellSharedLeafletFacet *> * _Nullable)facets reply:(InkwellSharedSharedLeafletCommentReplyRef * _Nullable)reply onPage:(NSString * _Nullable)onPage __attribute__((swift_name("doCopy(type:subject:createdAt:plaintext:facets:reply:onPage:)")));

/**
 * Neutral shared model for a `pub.leaflet.comment` record.
 *
 * Mirrors Android `LeafletComment` and iOS `PubLeafletComment`.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a `pub.leaflet.comment` record.
 *
 * Mirrors Android `LeafletComment` and iOS `PubLeafletComment`.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a `pub.leaflet.comment` record.
 *
 * Mirrors Android `LeafletComment` and iOS `PubLeafletComment`.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSArray<InkwellSharedLeafletFacet *> * _Nullable facets __attribute__((swift_name("facets")));
@property (readonly) NSString * _Nullable onPage __attribute__((swift_name("onPage")));
@property (readonly) NSString *plaintext __attribute__((swift_name("plaintext")));
@property (readonly) InkwellSharedSharedLeafletCommentReplyRef * _Nullable reply __attribute__((swift_name("reply")));
@property (readonly) NSString *subject __attribute__((swift_name("subject")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedLeafletComment.ReplyRef")))
@interface InkwellSharedSharedLeafletCommentReplyRef : InkwellSharedBase
- (instancetype)initWithParent:(NSString *)parent __attribute__((swift_name("init(parent:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedSharedLeafletCommentReplyRef *)doCopyParent:(NSString *)parent __attribute__((swift_name("doCopy(parent:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *parent __attribute__((swift_name("parent")));
@end


/**
 * Neutral shared model for a `site.standard.publication` record.
 *
 * Mirrors Android `PublicationRecord` and iOS `PublicationRecord`.
 * Only the fields needed by shared KMP logic and cross-platform mapping
 * are included here.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedPublicationRecord")))
@interface InkwellSharedSharedPublicationRecord : InkwellSharedBase
- (instancetype)initWithType:(NSString *)type url:(NSString *)url name:(NSString *)name description:(NSString * _Nullable)description icon:(InkwellSharedBlobRef * _Nullable)icon theme:(InkwellSharedPublicationTheme * _Nullable)theme basicTheme:(InkwellSharedBasicTheme * _Nullable)basicTheme preferences:(InkwellSharedPublicationPreferences * _Nullable)preferences __attribute__((swift_name("init(type:url:name:description:icon:theme:basicTheme:preferences:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedSharedPublicationRecord *)doCopyType:(NSString *)type url:(NSString *)url name:(NSString *)name description:(NSString * _Nullable)description icon:(InkwellSharedBlobRef * _Nullable)icon theme:(InkwellSharedPublicationTheme * _Nullable)theme basicTheme:(InkwellSharedBasicTheme * _Nullable)basicTheme preferences:(InkwellSharedPublicationPreferences * _Nullable)preferences __attribute__((swift_name("doCopy(type:url:name:description:icon:theme:basicTheme:preferences:)")));

/**
 * Neutral shared model for a `site.standard.publication` record.
 *
 * Mirrors Android `PublicationRecord` and iOS `PublicationRecord`.
 * Only the fields needed by shared KMP logic and cross-platform mapping
 * are included here.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for a `site.standard.publication` record.
 *
 * Mirrors Android `PublicationRecord` and iOS `PublicationRecord`.
 * Only the fields needed by shared KMP logic and cross-platform mapping
 * are included here.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for a `site.standard.publication` record.
 *
 * Mirrors Android `PublicationRecord` and iOS `PublicationRecord`.
 * Only the fields needed by shared KMP logic and cross-platform mapping
 * are included here.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) InkwellSharedBasicTheme * _Nullable basicTheme __attribute__((swift_name("basicTheme")));
@property (readonly) NSString * _Nullable description_ __attribute__((swift_name("description_")));
@property (readonly) InkwellSharedBlobRef * _Nullable icon __attribute__((swift_name("icon")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) InkwellSharedPublicationPreferences * _Nullable preferences __attribute__((swift_name("preferences")));
@property (readonly) InkwellSharedPublicationTheme * _Nullable theme __attribute__((swift_name("theme")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@property (readonly) NSString *url __attribute__((swift_name("url")));
@end


/**
 * Neutral shared model for an AT Protocol strong reference (URI + CID).
 *
 * Mirrors Android `StrongRef` and iOS `ComAtprotoLexicon.Repository.StrongReference`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StrongRef")))
@interface InkwellSharedStrongRef : InkwellSharedBase
- (instancetype)initWithUri:(NSString *)uri cid:(NSString * _Nullable)cid __attribute__((swift_name("init(uri:cid:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedStrongRef *)doCopyUri:(NSString *)uri cid:(NSString * _Nullable)cid __attribute__((swift_name("doCopy(uri:cid:)")));

/**
 * Neutral shared model for an AT Protocol strong reference (URI + CID).
 *
 * Mirrors Android `StrongRef` and iOS `ComAtprotoLexicon.Repository.StrongReference`.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Neutral shared model for an AT Protocol strong reference (URI + CID).
 *
 * Mirrors Android `StrongRef` and iOS `ComAtprotoLexicon.Repository.StrongReference`.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Neutral shared model for an AT Protocol strong reference (URI + CID).
 *
 * Mirrors Android `StrongRef` and iOS `ComAtprotoLexicon.Repository.StrongReference`.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable cid __attribute__((swift_name("cid")));
@property (readonly) NSString *uri __attribute__((swift_name("uri")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OAuthScopes")))
@interface InkwellSharedOAuthScopes : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)oAuthScopes __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedOAuthScopes *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *ATPROTO __attribute__((swift_name("ATPROTO")));
@property (readonly) NSString *AUTH_FULL __attribute__((swift_name("AUTH_FULL")));
@property (readonly) NSString *AUTH_SOCIAL __attribute__((swift_name("AUTH_SOCIAL")));
@property (readonly) NSString *BLOB_ALL __attribute__((swift_name("BLOB_ALL")));
@property (readonly) NSString *REPO_DOCUMENT __attribute__((swift_name("REPO_DOCUMENT")));
@property (readonly) NSString *REPO_PUBLICATION __attribute__((swift_name("REPO_PUBLICATION")));
@property (readonly) NSString *REPO_RECOMMEND __attribute__((swift_name("REPO_RECOMMEND")));
@property (readonly) NSString *REPO_SUBSCRIPTION __attribute__((swift_name("REPO_SUBSCRIPTION")));

/** Needed to post feedback to Inkwell's userinput.app board. */
@property (readonly) NSString *REPO_USERINPUT_DISCUSSION __attribute__((swift_name("REPO_USERINPUT_DISCUSSION")));
@end


/**
 * Shared notification polling policy constants and decision logic.
 *
 * The actual I/O (fetching subscriptions, documents, sending platform
 * notifications) stays native in each app. This object captures the
 * policy numbers and pure-decision logic that are identical on both
 * platforms.
 *
 * Mirrors iOS `NotificationManager.pollForNewDocuments` and Android
 * `InkwellNotificationManager.pollForNewDocuments` — same retention
 * numbers, same first-poll baseline, same sort order.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NotificationPolicy")))
@interface InkwellSharedNotificationPolicy : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Shared notification polling policy constants and decision logic.
 *
 * The actual I/O (fetching subscriptions, documents, sending platform
 * notifications) stays native in each app. This object captures the
 * policy numbers and pure-decision logic that are identical on both
 * platforms.
 *
 * Mirrors iOS `NotificationManager.pollForNewDocuments` and Android
 * `InkwellNotificationManager.pollForNewDocuments` — same retention
 * numbers, same first-poll baseline, same sort order.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)notificationPolicy __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedNotificationPolicy *shared __attribute__((swift_name("shared")));

/**
 * Returns true if this is the first poll (no previous poll timestamp).
 *
 * @param lastPollEpochMillis Epoch millis of the last poll, or -1 if never polled.
 */
- (BOOL)isFirstPollLastPollEpochMillis:(int64_t)lastPollEpochMillis __attribute__((swift_name("isFirstPoll(lastPollEpochMillis:)")));

/**
 * Determines whether to show a single-document notification or a summary.
 *
 * @param newDocCount Number of new documents discovered this poll.
 */
- (id<InkwellSharedNotificationStyle>)notificationStyleNewDocCount:(int32_t)newDocCount __attribute__((swift_name("notificationStyle(newDocCount:)")));

/**
 * Trims a notification list to at most [MAX_NOTIFICATIONS], keeping the newest.
 * The caller should pass notifications newest-first.
 */
- (NSArray<id> *)trimNotificationsNotifications:(NSArray<id> *)notifications __attribute__((swift_name("trimNotifications(notifications:)")));

/**
 * Trims a list of URIs to at most [MAX_SEEN_URIS], keeping the most recent.
 * The caller should pass URIs in chronological order (oldest first).
 */
- (NSArray<NSString *> *)trimSeenUrisSeenUris:(NSArray<NSString *> *)seenUris __attribute__((swift_name("trimSeenUris(seenUris:)")));

/** Maximum number of notifications to retain in the in-app list. */
@property (readonly) int32_t MAX_NOTIFICATIONS __attribute__((swift_name("MAX_NOTIFICATIONS")));

/** Maximum number of document URIs to remember as "seen". */
@property (readonly) int32_t MAX_SEEN_URIS __attribute__((swift_name("MAX_SEEN_URIS")));

/** Minimum number of new documents before showing a summary notification
 *  instead of individual per-document notifications. */
@property (readonly) int32_t SUMMARY_THRESHOLD __attribute__((swift_name("SUMMARY_THRESHOLD")));
@end

__attribute__((swift_name("NotificationStyle")))
@protocol InkwellSharedNotificationStyle
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NotificationStyleNone")))
@interface InkwellSharedNotificationStyleNone : InkwellSharedBase <InkwellSharedNotificationStyle>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)none __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedNotificationStyleNone *shared __attribute__((swift_name("shared")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NotificationStyleSingle")))
@interface InkwellSharedNotificationStyleSingle : InkwellSharedBase <InkwellSharedNotificationStyle>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)single __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedNotificationStyleSingle *shared __attribute__((swift_name("shared")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NotificationStyleSummary")))
@interface InkwellSharedNotificationStyleSummary : InkwellSharedBase <InkwellSharedNotificationStyle>
- (instancetype)initWithCount:(int32_t)count __attribute__((swift_name("init(count:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedNotificationStyleSummary *)doCopyCount:(int32_t)count __attribute__((swift_name("doCopy(count:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t count __attribute__((swift_name("count")));
@end


/**
 * Shared pagination policy for `com.atproto.repo.listRecords`.
 *
 * Both platforms page through a repo's records to completion, capped so a
 * misbehaving PDS returning an endless cursor can't hang the caller forever.
 * Previously iOS capped at 1,000 records and Android at 500 for the same
 * kind of call — this unifies the two.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordListPolicy")))
@interface InkwellSharedRecordListPolicy : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Shared pagination policy for `com.atproto.repo.listRecords`.
 *
 * Both platforms page through a repo's records to completion, capped so a
 * misbehaving PDS returning an endless cursor can't hang the caller forever.
 * Previously iOS capped at 1,000 records and Android at 500 for the same
 * kind of call — this unifies the two.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)recordListPolicy __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedRecordListPolicy *shared __attribute__((swift_name("shared")));

/** Maximum records to accumulate before giving up on further pages. */
@property (readonly) int32_t MAX_RECORDS __attribute__((swift_name("MAX_RECORDS")));

/** Records requested per page. */
@property (readonly) int32_t PAGE_LIMIT __attribute__((swift_name("PAGE_LIMIT")));
@end


/**
 * Pure tip-prompt gating policy — identical on both platforms.
 *
 * The prompt appears after [MIN_LAUNCHES] app launches and is suppressed for
 * [COOLDOWN_DAYS] after dismissal.
 *
 * Mirrors iOS `TipPromptManager` and Android `TipPromptManager` — same
 * thresholds, same cooldown logic.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TipPromptPolicy")))
@interface InkwellSharedTipPromptPolicy : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Pure tip-prompt gating policy — identical on both platforms.
 *
 * The prompt appears after [MIN_LAUNCHES] app launches and is suppressed for
 * [COOLDOWN_DAYS] after dismissal.
 *
 * Mirrors iOS `TipPromptManager` and Android `TipPromptManager` — same
 * thresholds, same cooldown logic.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)tipPromptPolicy __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedTipPromptPolicy *shared __attribute__((swift_name("shared")));

/**
 * Returns true if the tip prompt should be shown.
 *
 * @param launchCount Total number of recorded app launches.
 * @param lastShownEpochMillis Epoch millis of the last time the prompt was shown,
 *     or -1 if it has never been shown.
 * @param nowEpochMillis Current epoch millis (for testability).
 */
- (BOOL)shouldShowTipLaunchCount:(int32_t)launchCount lastShownEpochMillis:(int64_t)lastShownEpochMillis nowEpochMillis:(int64_t)nowEpochMillis __attribute__((swift_name("shouldShowTip(launchCount:lastShownEpochMillis:nowEpochMillis:)")));
@property (readonly) int64_t COOLDOWN_DAYS __attribute__((swift_name("COOLDOWN_DAYS")));
@property (readonly) int32_t MIN_LAUNCHES __attribute__((swift_name("MIN_LAUNCHES")));
@end


/**
 * Identifies Inkwell's Bluesky supporters list — people who've tipped via
 * Ko-fi or GitHub Sponsors, curated manually by ewancroft.uk. Not bridged
 * through the XCFramework (see [uk.ewancroft.inkwell.shared.feedback.UserInputLexicon]
 * for the same convention); iOS keeps its own literal copy in
 * `BSkyListFetcher.swift`.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SupportersList")))
@interface InkwellSharedSupportersList : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Identifies Inkwell's Bluesky supporters list — people who've tipped via
 * Ko-fi or GitHub Sponsors, curated manually by ewancroft.uk. Not bridged
 * through the XCFramework (see [uk.ewancroft.inkwell.shared.feedback.UserInputLexicon]
 * for the same convention); iOS keeps its own literal copy in
 * `BSkyListFetcher.swift`.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)supportersList __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedSupportersList *shared __attribute__((swift_name("shared")));

/** at://did:plc:ofrbh253gwicbkc5nktqepol/app.bsky.graph.list/3mtjkyzm3nx27 */
@property (readonly) NSString *URI __attribute__((swift_name("URI")));
@end


/**
 * Shared number formatting utility for displaying counts
 * (likes, reposts, replies) in abbreviated form.
 *
 * Used by both platforms' Bluesky post embed rendering.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NumberFormat")))
@interface InkwellSharedNumberFormat : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Shared number formatting utility for displaying counts
 * (likes, reposts, replies) in abbreviated form.
 *
 * Used by both platforms' Bluesky post embed rendering.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)numberFormat __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedNumberFormat *shared __attribute__((swift_name("shared")));

/**
 * Abbreviates a count: 1500000 → "1M", 2300 → "2K", 42 → "42".
 */
- (NSString *)formatCountCount:(int32_t)count __attribute__((swift_name("formatCount(count:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StringUtils")))
@interface InkwellSharedStringUtils : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)stringUtils __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedStringUtils *shared __attribute__((swift_name("shared")));
- (NSString *)trimTrailingSlashValue:(NSString *)value __attribute__((swift_name("trimTrailingSlash(value:)")));
@end


/**
 * Shared UTF-8 byte-offset ↔ character-index conversion.
 *
 * AT Protocol facet byte ranges are UTF-8 offsets, not platform
 * character indices. Both platforms need to map these to local
 * string indices for attributed-text rendering.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Utf8Offsets")))
@interface InkwellSharedUtf8Offsets : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Shared UTF-8 byte-offset ↔ character-index conversion.
 *
 * AT Protocol facet byte ranges are UTF-8 offsets, not platform
 * character indices. Both platforms need to map these to local
 * string indices for attributed-text rendering.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)utf8Offsets __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedUtf8Offsets *shared __attribute__((swift_name("shared")));

/**
 * Returns the total UTF-8 byte length of [text].
 */
- (int32_t)byteLengthText:(NSString *)text __attribute__((swift_name("byteLength(text:)")));

/**
 * Converts a UTF-8 byte range [byteStart, byteEnd) to a
 * character-index range within [text]. Returns [startChar, endChar]
 * inclusive — endChar is the character containing the byte at byteEnd.
 *
 * Returns null if the range is invalid or empty.
 */
- (InkwellSharedKotlinIntRange * _Nullable)byteRangeToCharRangeText:(NSString *)text byteStart:(int32_t)byteStart byteEnd:(int32_t)byteEnd __attribute__((swift_name("byteRangeToCharRange(text:byteStart:byteEnd:)")));

/**
 * Returns the cumulative UTF-8 byte offset of a character index
 * within [text]. Equivalent to counting bytes of all characters
 * before [charIndex].
 */
- (int32_t)charIndexToByteOffsetText:(NSString *)text charIndex:(int32_t)charIndex __attribute__((swift_name("charIndexToByteOffset(text:charIndex:)")));
@end


/**
 * Shared reader theme resolution — the cascade logic and font-family matching
 * that is identical on both platforms.
 *
 * Colors are stored as 0xRRGGBB Ints (no alpha) so both platforms can convert
 * them to their native Color type. The platform-specific ReaderTheme wrappers
 * call [resolve] and map the Ints to Compose Color / SwiftUI Color.
 *
 * Mirrors iOS `ReaderTheme` and Android `ReaderTheme` — same cascade:
 * Leaflet rich theme → legacy palette → basicTheme → system defaults.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedReaderTheme")))
@interface InkwellSharedSharedReaderTheme : InkwellSharedBase
- (instancetype)initWithBackgroundRgb:(int32_t)backgroundRgb pageBackgroundRgb:(int32_t)pageBackgroundRgb foregroundRgb:(int32_t)foregroundRgb accentRgb:(int32_t)accentRgb accentForegroundRgb:(int32_t)accentForegroundRgb pageWidthDp:(int32_t)pageWidthDp showPageBackground:(BOOL)showPageBackground headingFontFamily:(InkwellSharedSharedReaderThemeFontFamily *)headingFontFamily bodyFontFamily:(InkwellSharedSharedReaderThemeFontFamily *)bodyFontFamily __attribute__((swift_name("init(backgroundRgb:pageBackgroundRgb:foregroundRgb:accentRgb:accentForegroundRgb:pageWidthDp:showPageBackground:headingFontFamily:bodyFontFamily:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) InkwellSharedSharedReaderThemeCompanion *companion __attribute__((swift_name("companion")));
- (InkwellSharedSharedReaderTheme *)doCopyBackgroundRgb:(int32_t)backgroundRgb pageBackgroundRgb:(int32_t)pageBackgroundRgb foregroundRgb:(int32_t)foregroundRgb accentRgb:(int32_t)accentRgb accentForegroundRgb:(int32_t)accentForegroundRgb pageWidthDp:(int32_t)pageWidthDp showPageBackground:(BOOL)showPageBackground headingFontFamily:(InkwellSharedSharedReaderThemeFontFamily *)headingFontFamily bodyFontFamily:(InkwellSharedSharedReaderThemeFontFamily *)bodyFontFamily __attribute__((swift_name("doCopy(backgroundRgb:pageBackgroundRgb:foregroundRgb:accentRgb:accentForegroundRgb:pageWidthDp:showPageBackground:headingFontFamily:bodyFontFamily:)")));

/**
 * Shared reader theme resolution — the cascade logic and font-family matching
 * that is identical on both platforms.
 *
 * Colors are stored as 0xRRGGBB Ints (no alpha) so both platforms can convert
 * them to their native Color type. The platform-specific ReaderTheme wrappers
 * call [resolve] and map the Ints to Compose Color / SwiftUI Color.
 *
 * Mirrors iOS `ReaderTheme` and Android `ReaderTheme` — same cascade:
 * Leaflet rich theme → legacy palette → basicTheme → system defaults.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Shared reader theme resolution — the cascade logic and font-family matching
 * that is identical on both platforms.
 *
 * Colors are stored as 0xRRGGBB Ints (no alpha) so both platforms can convert
 * them to their native Color type. The platform-specific ReaderTheme wrappers
 * call [resolve] and map the Ints to Compose Color / SwiftUI Color.
 *
 * Mirrors iOS `ReaderTheme` and Android `ReaderTheme` — same cascade:
 * Leaflet rich theme → legacy palette → basicTheme → system defaults.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Shared reader theme resolution — the cascade logic and font-family matching
 * that is identical on both platforms.
 *
 * Colors are stored as 0xRRGGBB Ints (no alpha) so both platforms can convert
 * them to their native Color type. The platform-specific ReaderTheme wrappers
 * call [resolve] and map the Ints to Compose Color / SwiftUI Color.
 *
 * Mirrors iOS `ReaderTheme` and Android `ReaderTheme` — same cascade:
 * Leaflet rich theme → legacy palette → basicTheme → system defaults.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t accentForegroundRgb __attribute__((swift_name("accentForegroundRgb")));
@property (readonly) int32_t accentRgb __attribute__((swift_name("accentRgb")));
@property (readonly) int32_t backgroundRgb __attribute__((swift_name("backgroundRgb")));
@property (readonly) InkwellSharedSharedReaderThemeFontFamily *bodyFontFamily __attribute__((swift_name("bodyFontFamily")));
@property (readonly) int32_t foregroundRgb __attribute__((swift_name("foregroundRgb")));
@property (readonly) InkwellSharedSharedReaderThemeFontFamily *headingFontFamily __attribute__((swift_name("headingFontFamily")));
@property (readonly) int32_t pageBackgroundRgb __attribute__((swift_name("pageBackgroundRgb")));
@property (readonly) int32_t pageWidthDp __attribute__((swift_name("pageWidthDp")));
@property (readonly) BOOL showPageBackground __attribute__((swift_name("showPageBackground")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedReaderTheme.Companion")))
@interface InkwellSharedSharedReaderThemeCompanion : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedSharedReaderThemeCompanion *shared __attribute__((swift_name("shared")));
- (InkwellSharedSharedReaderThemeFontFamily *)fontFamilyForIdentifier:(NSString * _Nullable)identifier __attribute__((swift_name("fontFamilyFor(identifier:)")));
- (InkwellSharedInt * _Nullable)hexToRgbHex:(NSString *)hex __attribute__((swift_name("hexToRgb(hex:)")));

/** Standard perceived-luminance formula (Rec. 601): true below the
 *  midpoint, i.e. a color dark enough to need light content on top
 *  of it. [rgbInt] is 0xRRGGBB, matching this file's Int colour
 *  convention. */
- (BOOL)isPerceptuallyDarkRgbInt:(int32_t)rgbInt __attribute__((swift_name("isPerceptuallyDark(rgbInt:)")));
- (InkwellSharedSharedReaderTheme *)resolveRichBackgroundColor:(InkwellSharedInt * _Nullable)richBackgroundColor richPageBackgroundColor:(InkwellSharedInt * _Nullable)richPageBackgroundColor richPrimaryColor:(InkwellSharedInt * _Nullable)richPrimaryColor richAccentBackgroundColor:(InkwellSharedInt * _Nullable)richAccentBackgroundColor richAccentTextColor:(InkwellSharedInt * _Nullable)richAccentTextColor richPageWidth:(InkwellSharedInt * _Nullable)richPageWidth richShowPageBackground:(InkwellSharedBoolean * _Nullable)richShowPageBackground richHeadingFont:(NSString * _Nullable)richHeadingFont richBodyFont:(NSString * _Nullable)richBodyFont richSharedFont:(NSString * _Nullable)richSharedFont paletteBackground:(NSString * _Nullable)paletteBackground paletteText:(NSString * _Nullable)paletteText paletteLink:(NSString * _Nullable)paletteLink paletteAccent:(NSString * _Nullable)paletteAccent paletteSurfaceHover:(NSString * _Nullable)paletteSurfaceHover basicBackground:(NSString * _Nullable)basicBackground basicForeground:(NSString * _Nullable)basicForeground basicAccent:(NSString * _Nullable)basicAccent basicAccentForeground:(NSString * _Nullable)basicAccentForeground overrideAccentRgb:(InkwellSharedInt * _Nullable)overrideAccentRgb overrideFontFamily:(InkwellSharedSharedReaderThemeFontFamily * _Nullable)overrideFontFamily increaseContrast:(BOOL)increaseContrast __attribute__((swift_name("resolve(richBackgroundColor:richPageBackgroundColor:richPrimaryColor:richAccentBackgroundColor:richAccentTextColor:richPageWidth:richShowPageBackground:richHeadingFont:richBodyFont:richSharedFont:paletteBackground:paletteText:paletteLink:paletteAccent:paletteSurfaceHover:basicBackground:basicForeground:basicAccent:basicAccentForeground:overrideAccentRgb:overrideFontFamily:increaseContrast:)")));
@end

__attribute__((swift_name("KotlinComparable")))
@protocol InkwellSharedKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface InkwellSharedKotlinEnum<E> : InkwellSharedBase <InkwellSharedKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) InkwellSharedKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedReaderTheme.FontFamily")))
@interface InkwellSharedSharedReaderThemeFontFamily : InkwellSharedKotlinEnum<InkwellSharedSharedReaderThemeFontFamily *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) InkwellSharedSharedReaderThemeFontFamily *sans __attribute__((swift_name("sans")));
@property (class, readonly) InkwellSharedSharedReaderThemeFontFamily *serif __attribute__((swift_name("serif")));
@property (class, readonly) InkwellSharedSharedReaderThemeFontFamily *rounded __attribute__((swift_name("rounded")));
@property (class, readonly) InkwellSharedSharedReaderThemeFontFamily *monospaced __attribute__((swift_name("monospaced")));
+ (InkwellSharedKotlinArray<InkwellSharedSharedReaderThemeFontFamily *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<InkwellSharedSharedReaderThemeFontFamily *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UrlUtils")))
@interface InkwellSharedUrlUtils : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)urlUtils __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedUrlUtils *shared __attribute__((swift_name("shared")));

/**
 * Builds the canonical web URL described by Standard.site's `site` + `path`
 * rules. Returns null if `site` is not a valid HTTPS URL and no publication
 * URL is provided for AT-URI sites.
 */
- (NSString * _Nullable)canonicalUrlSite:(NSString *)site path:(NSString * _Nullable)path publicationUrl:(NSString * _Nullable)publicationUrl __attribute__((swift_name("canonicalUrl(site:path:publicationUrl:)")));

/**
 * Normalizes a URL: lowercases scheme and host, trims trailing slashes.
 * Returns the original string if it can't be parsed as a URL.
 */
- (NSString *)normalizedSiteValue:(NSString *)value __attribute__((swift_name("normalizedSite(value:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("HandleUtils")))
@interface InkwellSharedHandleUtils : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)handleUtils __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedHandleUtils *shared __attribute__((swift_name("shared")));
- (NSString *)normalizeHandle:(NSString *)handle __attribute__((swift_name("normalize(handle:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DocumentLinkScanner")))
@interface InkwellSharedDocumentLinkScanner : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)documentLinkScanner __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedDocumentLinkScanner *shared __attribute__((swift_name("shared")));

/**
 * Regex-based `<link>` search, tolerant of either attribute order and quote style.
 * Mirrors the iOS and Android implementations' approach rather than pulling in
 * an HTML parser.
 */
- (BOOL)containsDocumentLinkHtml:(NSString *)html documentURI:(NSString *)documentURI __attribute__((swift_name("containsDocumentLink(html:documentURI:)")));
@end


/**
 * Why a publication or document failed verification.
 * Kept as distinct, diagnosable cases so the UI and logs can say
 * why a record is untrusted.
 */
__attribute__((swift_name("VerificationFailure")))
@interface InkwellSharedVerificationFailure : InkwellSharedBase
@property (readonly) NSString *reason __attribute__((swift_name("reason")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VerificationFailure.DocumentLinkMissing")))
@interface InkwellSharedVerificationFailureDocumentLinkMissing : InkwellSharedVerificationFailure
- (instancetype)initWithExpected:(NSString *)expected __attribute__((swift_name("init(expected:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedVerificationFailureDocumentLinkMissing *)doCopyExpected:(NSString *)expected __attribute__((swift_name("doCopy(expected:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *expected __attribute__((swift_name("expected")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VerificationFailure.EndpointUnreachable")))
@interface InkwellSharedVerificationFailureEndpointUnreachable : InkwellSharedVerificationFailure
- (instancetype)initWithStatusCode:(InkwellSharedInt * _Nullable)statusCode __attribute__((swift_name("init(statusCode:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedVerificationFailureEndpointUnreachable *)doCopyStatusCode:(InkwellSharedInt * _Nullable)statusCode __attribute__((swift_name("doCopy(statusCode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) InkwellSharedInt * _Nullable statusCode __attribute__((swift_name("statusCode")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VerificationFailure.InvalidDocumentURL")))
@interface InkwellSharedVerificationFailureInvalidDocumentURL : InkwellSharedVerificationFailure
- (instancetype)initWithUrl:(NSString *)url __attribute__((swift_name("init(url:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedVerificationFailureInvalidDocumentURL *)doCopyUrl:(NSString *)url __attribute__((swift_name("doCopy(url:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *url __attribute__((swift_name("url")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VerificationFailure.InvalidPublicationURL")))
@interface InkwellSharedVerificationFailureInvalidPublicationURL : InkwellSharedVerificationFailure
- (instancetype)initWithUrl:(NSString *)url __attribute__((swift_name("init(url:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedVerificationFailureInvalidPublicationURL *)doCopyUrl:(NSString *)url __attribute__((swift_name("doCopy(url:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *url __attribute__((swift_name("url")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VerificationFailure.MalformedResponse")))
@interface InkwellSharedVerificationFailureMalformedResponse : InkwellSharedVerificationFailure
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)malformedResponse __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedVerificationFailureMalformedResponse *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VerificationFailure.MismatchedURI")))
@interface InkwellSharedVerificationFailureMismatchedURI : InkwellSharedVerificationFailure
- (instancetype)initWithExpected:(NSString *)expected found:(NSString *)found __attribute__((swift_name("init(expected:found:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedVerificationFailureMismatchedURI *)doCopyExpected:(NSString *)expected found:(NSString *)found __attribute__((swift_name("doCopy(expected:found:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *expected __attribute__((swift_name("expected")));
@property (readonly) NSString *found __attribute__((swift_name("found")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VerificationFailure.Unexpected")))
@interface InkwellSharedVerificationFailureUnexpected : InkwellSharedVerificationFailure
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedVerificationFailureUnexpected *)doCopyMessage:(NSString * _Nullable)message __attribute__((swift_name("doCopy(message:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
@end

__attribute__((swift_name("VerificationResult")))
@interface InkwellSharedVerificationResult : InkwellSharedBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VerificationResult.Failed")))
@interface InkwellSharedVerificationResultFailed : InkwellSharedVerificationResult
- (instancetype)initWithFailure:(InkwellSharedVerificationFailure *)failure __attribute__((swift_name("init(failure:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedVerificationResultFailed *)doCopyFailure:(InkwellSharedVerificationFailure *)failure __attribute__((swift_name("doCopy(failure:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) InkwellSharedVerificationFailure *failure __attribute__((swift_name("failure")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VerificationResult.Verified")))
@interface InkwellSharedVerificationResultVerified : InkwellSharedVerificationResult
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)verified __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedVerificationResultVerified *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VerificationUrls")))
@interface InkwellSharedVerificationUrls : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)verificationUrls __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedVerificationUrls *shared __attribute__((swift_name("shared")));

/**
 * Builds the `<link>` discovery tag a document page must serve in its
 * `<head>` to point back at its AT-URI record.
 */
- (NSString *)discoveryLinkTagRecordURI:(NSString *)recordURI relation:(NSString *)relation __attribute__((swift_name("discoveryLinkTag(recordURI:relation:)")));

/**
 * Builds the canonical web URL for a document per standard.site's `site` + `path`
 * rules. A resolved publication URL is required when `documentSite` is an AT-URI
 * (i.e. the document belongs to a publication) rather than a direct `https://` URL.
 */
- (NSString * _Nullable)documentCanonicalUrlDocumentSite:(NSString *)documentSite documentPath:(NSString * _Nullable)documentPath publicationUrl:(NSString * _Nullable)publicationUrl __attribute__((swift_name("documentCanonicalUrl(documentSite:documentPath:publicationUrl:)")));

/**
 * Builds the `.well-known` verification endpoint for a publication, including the
 * publication's own path for non-root publications — e.g. a publication living at
 * `https://example.com/writing` verifies at
 * `https://example.com/.well-known/site.standard.publication/writing`.
 */
- (NSString * _Nullable)publicationVerificationUrlPublicationUrl:(NSString *)publicationUrl __attribute__((swift_name("publicationVerificationUrl(publicationUrl:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("XrpcEndpoints")))
@interface InkwellSharedXrpcEndpoints : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)xrpcEndpoints __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedXrpcEndpoints *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *ACTOR_GET_PROFILE __attribute__((swift_name("ACTOR_GET_PROFILE")));
@property (readonly) NSString *CONSTELLATION_API __attribute__((swift_name("CONSTELLATION_API")));
@property (readonly) NSString *FEED_GET_POSTS __attribute__((swift_name("FEED_GET_POSTS")));
@property (readonly) NSString *GRAPH_GET_LIST __attribute__((swift_name("GRAPH_GET_LIST")));
@property (readonly) NSString *IDENTITY_RESOLVE_HANDLE __attribute__((swift_name("IDENTITY_RESOLVE_HANDLE")));
@property (readonly) NSString *MICROCOSM_GET_BACKLINKS __attribute__((swift_name("MICROCOSM_GET_BACKLINKS")));
@property (readonly) NSString *PUBLIC_BSKY_API __attribute__((swift_name("PUBLIC_BSKY_API")));
@property (readonly) NSString *REPO_CREATE_RECORD __attribute__((swift_name("REPO_CREATE_RECORD")));
@property (readonly) NSString *REPO_DELETE_RECORD __attribute__((swift_name("REPO_DELETE_RECORD")));
@property (readonly) NSString *REPO_GET_RECORD __attribute__((swift_name("REPO_GET_RECORD")));
@property (readonly) NSString *REPO_LIST_RECORDS __attribute__((swift_name("REPO_LIST_RECORDS")));
@property (readonly) NSString *REPO_PUT_RECORD __attribute__((swift_name("REPO_PUT_RECORD")));
@property (readonly) NSString *REPO_UPLOAD_BLOB __attribute__((swift_name("REPO_UPLOAD_BLOB")));
@property (readonly) NSString *SERVER_GET_SESSION __attribute__((swift_name("SERVER_GET_SESSION")));
@property (readonly) NSString *SYNC_GET_BLOB __attribute__((swift_name("SYNC_GET_BLOB")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerializationStrategy")))
@protocol InkwellSharedKotlinx_serialization_coreSerializationStrategy
@required
- (void)serializeEncoder:(id<InkwellSharedKotlinx_serialization_coreEncoder>)encoder value:(id _Nullable)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<InkwellSharedKotlinx_serialization_coreSerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreDeserializationStrategy")))
@protocol InkwellSharedKotlinx_serialization_coreDeserializationStrategy
@required
- (id _Nullable)deserializeDecoder:(id<InkwellSharedKotlinx_serialization_coreDecoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
@property (readonly) id<InkwellSharedKotlinx_serialization_coreSerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreKSerializer")))
@protocol InkwellSharedKotlinx_serialization_coreKSerializer <InkwellSharedKotlinx_serialization_coreSerializationStrategy, InkwellSharedKotlinx_serialization_coreDeserializationStrategy>
@required
@end

__attribute__((swift_name("KotlinThrowable")))
@interface InkwellSharedKotlinThrowable : InkwellSharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(InkwellSharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(InkwellSharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
- (InkwellSharedKotlinArray<NSString *> *)getStackTrace __attribute__((swift_name("getStackTrace()")));
- (void)printStackTrace __attribute__((swift_name("printStackTrace()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) InkwellSharedKotlinThrowable * _Nullable cause __attribute__((swift_name("cause")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (NSError *)asError __attribute__((swift_name("asError()")));
@end

__attribute__((swift_name("KotlinException")))
@interface InkwellSharedKotlinException : InkwellSharedKotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(InkwellSharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(InkwellSharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinRuntimeException")))
@interface InkwellSharedKotlinRuntimeException : InkwellSharedKotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(InkwellSharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(InkwellSharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinIllegalStateException")))
@interface InkwellSharedKotlinIllegalStateException : InkwellSharedKotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(InkwellSharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(InkwellSharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
__attribute__((swift_name("KotlinCancellationException")))
@interface InkwellSharedKotlinCancellationException : InkwellSharedKotlinIllegalStateException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(InkwellSharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(InkwellSharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinFunction")))
@protocol InkwellSharedKotlinFunction
@required
@end

__attribute__((swift_name("KotlinSuspendFunction2")))
@protocol InkwellSharedKotlinSuspendFunction2 <InkwellSharedKotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 p2:(id _Nullable)p2 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:p2:completionHandler:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/serialization/json/JsonElementSerializer))
*/
__attribute__((swift_name("Kotlinx_serialization_jsonJsonElement")))
@interface InkwellSharedKotlinx_serialization_jsonJsonElement : InkwellSharedBase
@property (class, readonly, getter=companion) InkwellSharedKotlinx_serialization_jsonJsonElementCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinPair")))
@interface InkwellSharedKotlinPair<__covariant A, __covariant B> : InkwellSharedBase
- (instancetype)initWithFirst:(A _Nullable)first second:(B _Nullable)second __attribute__((swift_name("init(first:second:)"))) __attribute__((objc_designated_initializer));
- (InkwellSharedKotlinPair<A, B> *)doCopyFirst:(A _Nullable)first second:(B _Nullable)second __attribute__((swift_name("doCopy(first:second:)")));
- (BOOL)equalsOther:(id _Nullable)other __attribute__((swift_name("equals(other:)")));
- (int32_t)hashCode __attribute__((swift_name("hashCode()")));
- (NSString *)toString __attribute__((swift_name("toString()")));
@property (readonly) A _Nullable first __attribute__((swift_name("first")));
@property (readonly) B _Nullable second __attribute__((swift_name("second")));
@end

__attribute__((swift_name("KotlinIterable")))
@protocol InkwellSharedKotlinIterable
@required
- (id<InkwellSharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
@end

__attribute__((swift_name("KotlinIntProgression")))
@interface InkwellSharedKotlinIntProgression : InkwellSharedBase <InkwellSharedKotlinIterable>
@property (class, readonly, getter=companion) InkwellSharedKotlinIntProgressionCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
- (InkwellSharedKotlinIntIterator *)iterator __attribute__((swift_name("iterator()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t first __attribute__((swift_name("first")));
@property (readonly) int32_t last __attribute__((swift_name("last")));
@property (readonly) int32_t step __attribute__((swift_name("step")));
@end

__attribute__((swift_name("KotlinClosedRange")))
@protocol InkwellSharedKotlinClosedRange
@required
- (BOOL)containsValue:(id)value __attribute__((swift_name("contains(value:)")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
@property (readonly) id endInclusive __attribute__((swift_name("endInclusive")));
@property (readonly) id start __attribute__((swift_name("start")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.9")
*/
__attribute__((swift_name("KotlinOpenEndRange")))
@protocol InkwellSharedKotlinOpenEndRange
@required
- (BOOL)containsValue_:(id)value __attribute__((swift_name("contains(value_:)")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
@property (readonly) id endExclusive __attribute__((swift_name("endExclusive")));
@property (readonly) id start __attribute__((swift_name("start")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinIntRange")))
@interface InkwellSharedKotlinIntRange : InkwellSharedKotlinIntProgression <InkwellSharedKotlinClosedRange, InkwellSharedKotlinOpenEndRange>
- (instancetype)initWithStart:(int32_t)start endInclusive:(int32_t)endInclusive __attribute__((swift_name("init(start:endInclusive:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) InkwellSharedKotlinIntRangeCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)containsValue:(InkwellSharedInt *)value __attribute__((swift_name("contains(value:)")));
- (BOOL)containsValue_:(InkwellSharedInt *)value __attribute__((swift_name("contains(value_:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.9")
*/
@property (readonly) InkwellSharedInt *endExclusive __attribute__((swift_name("endExclusive"))) __attribute__((deprecated("Can throw an exception when it's impossible to represent the value with Int type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")));
@property (readonly) InkwellSharedInt *endInclusive __attribute__((swift_name("endInclusive")));
@property (readonly) InkwellSharedInt *start __attribute__((swift_name("start")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface InkwellSharedKotlinEnumCompanion : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface InkwellSharedKotlinArray<T> : InkwellSharedBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(InkwellSharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<InkwellSharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreEncoder")))
@protocol InkwellSharedKotlinx_serialization_coreEncoder
@required
- (id<InkwellSharedKotlinx_serialization_coreCompositeEncoder>)beginCollectionDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor collectionSize:(int32_t)collectionSize __attribute__((swift_name("beginCollection(descriptor:collectionSize:)")));
- (id<InkwellSharedKotlinx_serialization_coreCompositeEncoder>)beginStructureDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (void)encodeBooleanValue:(BOOL)value __attribute__((swift_name("encodeBoolean(value:)")));
- (void)encodeByteValue:(int8_t)value __attribute__((swift_name("encodeByte(value:)")));
- (void)encodeCharValue:(unichar)value __attribute__((swift_name("encodeChar(value:)")));
- (void)encodeDoubleValue:(double)value __attribute__((swift_name("encodeDouble(value:)")));
- (void)encodeEnumEnumDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)enumDescriptor index:(int32_t)index __attribute__((swift_name("encodeEnum(enumDescriptor:index:)")));
- (void)encodeFloatValue:(float)value __attribute__((swift_name("encodeFloat(value:)")));
- (id<InkwellSharedKotlinx_serialization_coreEncoder>)encodeInlineDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("encodeInline(descriptor:)")));
- (void)encodeIntValue:(int32_t)value __attribute__((swift_name("encodeInt(value:)")));
- (void)encodeLongValue:(int64_t)value __attribute__((swift_name("encodeLong(value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNotNullMark __attribute__((swift_name("encodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNull __attribute__((swift_name("encodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableValueSerializer:(id<InkwellSharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableValue(serializer:value:)")));
- (void)encodeSerializableValueSerializer:(id<InkwellSharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableValue(serializer:value:)")));
- (void)encodeShortValue:(int16_t)value __attribute__((swift_name("encodeShort(value:)")));
- (void)encodeStringValue:(NSString *)value __attribute__((swift_name("encodeString(value:)")));
@property (readonly) InkwellSharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerialDescriptor")))
@protocol InkwellSharedKotlinx_serialization_coreSerialDescriptor
@required

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (NSArray<id<InkwellSharedKotlinAnnotation>> *)getElementAnnotationsIndex:(int32_t)index __attribute__((swift_name("getElementAnnotations(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)getElementDescriptorIndex:(int32_t)index __attribute__((swift_name("getElementDescriptor(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (int32_t)getElementIndexName:(NSString *)name __attribute__((swift_name("getElementIndex(name:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (NSString *)getElementNameIndex:(int32_t)index __attribute__((swift_name("getElementName(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)isElementOptionalIndex:(int32_t)index __attribute__((swift_name("isElementOptional(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) NSArray<id<InkwellSharedKotlinAnnotation>> *annotations __attribute__((swift_name("annotations")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) int32_t elementsCount __attribute__((swift_name("elementsCount")));
@property (readonly) BOOL isInline __attribute__((swift_name("isInline")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) BOOL isNullable __attribute__((swift_name("isNullable")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) InkwellSharedKotlinx_serialization_coreSerialKind *kind __attribute__((swift_name("kind")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) NSString *serialName __attribute__((swift_name("serialName")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreDecoder")))
@protocol InkwellSharedKotlinx_serialization_coreDecoder
@required
- (id<InkwellSharedKotlinx_serialization_coreCompositeDecoder>)beginStructureDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (BOOL)decodeBoolean __attribute__((swift_name("decodeBoolean()")));
- (int8_t)decodeByte __attribute__((swift_name("decodeByte()")));
- (unichar)decodeChar __attribute__((swift_name("decodeChar()")));
- (double)decodeDouble __attribute__((swift_name("decodeDouble()")));
- (int32_t)decodeEnumEnumDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeEnum(enumDescriptor:)")));
- (float)decodeFloat __attribute__((swift_name("decodeFloat()")));
- (id<InkwellSharedKotlinx_serialization_coreDecoder>)decodeInlineDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeInline(descriptor:)")));
- (int32_t)decodeInt __attribute__((swift_name("decodeInt()")));
- (int64_t)decodeLong __attribute__((swift_name("decodeLong()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeNotNullMark __attribute__((swift_name("decodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (InkwellSharedKotlinNothing * _Nullable)decodeNull __attribute__((swift_name("decodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableValueDeserializer:(id<InkwellSharedKotlinx_serialization_coreDeserializationStrategy>)deserializer __attribute__((swift_name("decodeNullableSerializableValue(deserializer:)")));
- (id _Nullable)decodeSerializableValueDeserializer:(id<InkwellSharedKotlinx_serialization_coreDeserializationStrategy>)deserializer __attribute__((swift_name("decodeSerializableValue(deserializer:)")));
- (int16_t)decodeShort __attribute__((swift_name("decodeShort()")));
- (NSString *)decodeString __attribute__((swift_name("decodeString()")));
@property (readonly) InkwellSharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_jsonJsonElement.Companion")))
@interface InkwellSharedKotlinx_serialization_jsonJsonElementCompanion : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedKotlinx_serialization_jsonJsonElementCompanion *shared __attribute__((swift_name("shared")));
- (id<InkwellSharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol InkwellSharedKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinIntProgression.Companion")))
@interface InkwellSharedKotlinIntProgressionCompanion : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedKotlinIntProgressionCompanion *shared __attribute__((swift_name("shared")));
- (InkwellSharedKotlinIntProgression *)fromClosedRangeRangeStart:(int32_t)rangeStart rangeEnd:(int32_t)rangeEnd step:(int32_t)step __attribute__((swift_name("fromClosedRange(rangeStart:rangeEnd:step:)")));
@end

__attribute__((swift_name("KotlinIntIterator")))
@interface InkwellSharedKotlinIntIterator : InkwellSharedBase <InkwellSharedKotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (InkwellSharedInt *)next __attribute__((swift_name("next()")));
- (int32_t)nextInt __attribute__((swift_name("nextInt()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinIntRange.Companion")))
@interface InkwellSharedKotlinIntRangeCompanion : InkwellSharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) InkwellSharedKotlinIntRangeCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) InkwellSharedKotlinIntRange *EMPTY __attribute__((swift_name("EMPTY")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreCompositeEncoder")))
@protocol InkwellSharedKotlinx_serialization_coreCompositeEncoder
@required
- (void)encodeBooleanElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(BOOL)value __attribute__((swift_name("encodeBooleanElement(descriptor:index:value:)")));
- (void)encodeByteElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int8_t)value __attribute__((swift_name("encodeByteElement(descriptor:index:value:)")));
- (void)encodeCharElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(unichar)value __attribute__((swift_name("encodeCharElement(descriptor:index:value:)")));
- (void)encodeDoubleElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(double)value __attribute__((swift_name("encodeDoubleElement(descriptor:index:value:)")));
- (void)encodeFloatElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(float)value __attribute__((swift_name("encodeFloatElement(descriptor:index:value:)")));
- (id<InkwellSharedKotlinx_serialization_coreEncoder>)encodeInlineElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeInlineElement(descriptor:index:)")));
- (void)encodeIntElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int32_t)value __attribute__((swift_name("encodeIntElement(descriptor:index:value:)")));
- (void)encodeLongElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int64_t)value __attribute__((swift_name("encodeLongElement(descriptor:index:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index serializer:(id<InkwellSharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeSerializableElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index serializer:(id<InkwellSharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeShortElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int16_t)value __attribute__((swift_name("encodeShortElement(descriptor:index:value:)")));
- (void)encodeStringElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(NSString *)value __attribute__((swift_name("encodeStringElement(descriptor:index:value:)")));
- (void)endStructureDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)shouldEncodeElementDefaultDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("shouldEncodeElementDefault(descriptor:index:)")));
@property (readonly) InkwellSharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerializersModule")))
@interface InkwellSharedKotlinx_serialization_coreSerializersModule : InkwellSharedBase

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)dumpToCollector:(id<InkwellSharedKotlinx_serialization_coreSerializersModuleCollector>)collector __attribute__((swift_name("dumpTo(collector:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<InkwellSharedKotlinx_serialization_coreKSerializer> _Nullable)getContextualKClass:(id<InkwellSharedKotlinKClass>)kClass typeArgumentsSerializers:(NSArray<id<InkwellSharedKotlinx_serialization_coreKSerializer>> *)typeArgumentsSerializers __attribute__((swift_name("getContextual(kClass:typeArgumentsSerializers:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<InkwellSharedKotlinx_serialization_coreSerializationStrategy> _Nullable)getPolymorphicBaseClass:(id<InkwellSharedKotlinKClass>)baseClass value:(id)value __attribute__((swift_name("getPolymorphic(baseClass:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<InkwellSharedKotlinx_serialization_coreDeserializationStrategy> _Nullable)getPolymorphicBaseClass:(id<InkwellSharedKotlinKClass>)baseClass serializedClassName:(NSString * _Nullable)serializedClassName __attribute__((swift_name("getPolymorphic(baseClass:serializedClassName:)")));
@end

__attribute__((swift_name("KotlinAnnotation")))
@protocol InkwellSharedKotlinAnnotation
@required
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((swift_name("Kotlinx_serialization_coreSerialKind")))
@interface InkwellSharedKotlinx_serialization_coreSerialKind : InkwellSharedBase
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreCompositeDecoder")))
@protocol InkwellSharedKotlinx_serialization_coreCompositeDecoder
@required
- (BOOL)decodeBooleanElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeBooleanElement(descriptor:index:)")));
- (int8_t)decodeByteElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeByteElement(descriptor:index:)")));
- (unichar)decodeCharElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeCharElement(descriptor:index:)")));
- (int32_t)decodeCollectionSizeDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeCollectionSize(descriptor:)")));
- (double)decodeDoubleElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeDoubleElement(descriptor:index:)")));
- (int32_t)decodeElementIndexDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeElementIndex(descriptor:)")));
- (float)decodeFloatElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeFloatElement(descriptor:index:)")));
- (id<InkwellSharedKotlinx_serialization_coreDecoder>)decodeInlineElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeInlineElement(descriptor:index:)")));
- (int32_t)decodeIntElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeIntElement(descriptor:index:)")));
- (int64_t)decodeLongElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeLongElement(descriptor:index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<InkwellSharedKotlinx_serialization_coreDeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeNullableSerializableElement(descriptor:index:deserializer:previousValue:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeSequentially __attribute__((swift_name("decodeSequentially()")));
- (id _Nullable)decodeSerializableElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<InkwellSharedKotlinx_serialization_coreDeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (int16_t)decodeShortElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeShortElement(descriptor:index:)")));
- (NSString *)decodeStringElementDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeStringElement(descriptor:index:)")));
- (void)endStructureDescriptor:(id<InkwellSharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
@property (readonly) InkwellSharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinNothing")))
@interface InkwellSharedKotlinNothing : InkwellSharedBase
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((swift_name("Kotlinx_serialization_coreSerializersModuleCollector")))
@protocol InkwellSharedKotlinx_serialization_coreSerializersModuleCollector
@required
- (void)contextualKClass:(id<InkwellSharedKotlinKClass>)kClass provider:(id<InkwellSharedKotlinx_serialization_coreKSerializer> (^)(NSArray<id<InkwellSharedKotlinx_serialization_coreKSerializer>> *typeArgumentsSerializers))provider __attribute__((swift_name("contextual(kClass:provider:)")));
- (void)contextualKClass:(id<InkwellSharedKotlinKClass>)kClass serializer:(id<InkwellSharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("contextual(kClass:serializer:)")));
- (void)polymorphicBaseClass:(id<InkwellSharedKotlinKClass>)baseClass actualClass:(id<InkwellSharedKotlinKClass>)actualClass actualSerializer:(id<InkwellSharedKotlinx_serialization_coreKSerializer>)actualSerializer __attribute__((swift_name("polymorphic(baseClass:actualClass:actualSerializer:)")));
- (void)polymorphicDefaultBaseClass:(id<InkwellSharedKotlinKClass>)baseClass defaultDeserializerProvider:(id<InkwellSharedKotlinx_serialization_coreDeserializationStrategy> _Nullable (^)(NSString * _Nullable className))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefault(baseClass:defaultDeserializerProvider:)"))) __attribute__((deprecated("Deprecated in favor of function with more precise name: polymorphicDefaultDeserializer")));
- (void)polymorphicDefaultDeserializerBaseClass:(id<InkwellSharedKotlinKClass>)baseClass defaultDeserializerProvider:(id<InkwellSharedKotlinx_serialization_coreDeserializationStrategy> _Nullable (^)(NSString * _Nullable className))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefaultDeserializer(baseClass:defaultDeserializerProvider:)")));
- (void)polymorphicDefaultSerializerBaseClass:(id<InkwellSharedKotlinKClass>)baseClass defaultSerializerProvider:(id<InkwellSharedKotlinx_serialization_coreSerializationStrategy> _Nullable (^)(id value))defaultSerializerProvider __attribute__((swift_name("polymorphicDefaultSerializer(baseClass:defaultSerializerProvider:)")));
@end

__attribute__((swift_name("KotlinKDeclarationContainer")))
@protocol InkwellSharedKotlinKDeclarationContainer
@required
@end

__attribute__((swift_name("KotlinKAnnotatedElement")))
@protocol InkwellSharedKotlinKAnnotatedElement
@required
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
__attribute__((swift_name("KotlinKClassifier")))
@protocol InkwellSharedKotlinKClassifier
@required
@end

__attribute__((swift_name("KotlinKClass")))
@protocol InkwellSharedKotlinKClass <InkwellSharedKotlinKDeclarationContainer, InkwellSharedKotlinKAnnotatedElement, InkwellSharedKotlinKClassifier>
@required

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
- (BOOL)isInstanceValue:(id _Nullable)value __attribute__((swift_name("isInstance(value:)")));
@property (readonly) NSString * _Nullable qualifiedName __attribute__((swift_name("qualifiedName")));
@property (readonly) NSString * _Nullable simpleName __attribute__((swift_name("simpleName")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
