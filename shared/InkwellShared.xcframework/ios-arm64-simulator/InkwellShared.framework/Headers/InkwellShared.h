#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class FacetConverter, InkwellSharedAtUri, InkwellSharedAtUriCompanion, InkwellSharedConstellationBacklink, InkwellSharedConstellationPagination, InkwellSharedConstellationResponse, InkwellSharedDocumentLinkScanner, InkwellSharedFacetDefinition, InkwellSharedFacetSchema, InkwellSharedKotlinArray<T>, InkwellSharedKotlinEnum<E>, InkwellSharedKotlinEnumCompanion, InkwellSharedKotlinException, InkwellSharedKotlinIllegalStateException, InkwellSharedKotlinNothing, InkwellSharedKotlinPair<__covariant A, __covariant B>, InkwellSharedKotlinRuntimeException, InkwellSharedKotlinThrowable, InkwellSharedKotlinx_serialization_coreSerialKind, InkwellSharedKotlinx_serialization_coreSerializersModule, InkwellSharedMarkdownBlock, InkwellSharedMarkdownBlockBlockquote, InkwellSharedMarkdownBlockCode, InkwellSharedMarkdownBlockHeading, InkwellSharedMarkdownBlockHorizontalRule, InkwellSharedMarkdownBlockImage, InkwellSharedMarkdownBlockMath, InkwellSharedMarkdownBlockOrderedList, InkwellSharedMarkdownBlockParagraph, InkwellSharedMarkdownBlockTaskList, InkwellSharedMarkdownBlockUnorderedList, InkwellSharedMarkdownListItem, InkwellSharedMarkdownParser, InkwellSharedMarkdownSerializer, InkwellSharedNotificationPolicy, InkwellSharedNotificationStyleNone, InkwellSharedNotificationStyleSingle, InkwellSharedNotificationStyleSummary, InkwellSharedSharedReaderTheme, InkwellSharedSharedReaderThemeCompanion, InkwellSharedSharedReaderThemeFontFamily, InkwellSharedTipPromptPolicy, InkwellSharedUrlUtils, InkwellSharedVerificationFailure, InkwellSharedVerificationFailureDocumentLinkMissing, InkwellSharedVerificationFailureEndpointUnreachable, InkwellSharedVerificationFailureInvalidDocumentURL, InkwellSharedVerificationFailureInvalidPublicationURL, InkwellSharedVerificationFailureMalformedResponse, InkwellSharedVerificationFailureMismatchedURI, InkwellSharedVerificationFailureUnexpected, InkwellSharedVerificationResult, InkwellSharedVerificationResultFailed, InkwellSharedVerificationResultVerified, InkwellSharedVerificationUrls, RichTextFacet, RichTextFeature;

@protocol InkwellSharedKotlinAnnotation, InkwellSharedKotlinComparable, InkwellSharedKotlinFunction, InkwellSharedKotlinIterator, InkwellSharedKotlinKAnnotatedElement, InkwellSharedKotlinKClass, InkwellSharedKotlinKClassifier, InkwellSharedKotlinKDeclarationContainer, InkwellSharedKotlinSuspendFunction2, InkwellSharedKotlinx_serialization_coreCompositeDecoder, InkwellSharedKotlinx_serialization_coreCompositeEncoder, InkwellSharedKotlinx_serialization_coreDecoder, InkwellSharedKotlinx_serialization_coreDeserializationStrategy, InkwellSharedKotlinx_serialization_coreEncoder, InkwellSharedKotlinx_serialization_coreKSerializer, InkwellSharedKotlinx_serialization_coreSerialDescriptor, InkwellSharedKotlinx_serialization_coreSerializationStrategy, InkwellSharedKotlinx_serialization_coreSerializersModuleCollector, InkwellSharedNotificationStyle;

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
- (InkwellSharedSharedReaderTheme *)resolveRichBackgroundColor:(InkwellSharedInt * _Nullable)richBackgroundColor richPageBackgroundColor:(InkwellSharedInt * _Nullable)richPageBackgroundColor richPrimaryColor:(InkwellSharedInt * _Nullable)richPrimaryColor richAccentBackgroundColor:(InkwellSharedInt * _Nullable)richAccentBackgroundColor richAccentTextColor:(InkwellSharedInt * _Nullable)richAccentTextColor richPageWidth:(InkwellSharedInt * _Nullable)richPageWidth richShowPageBackground:(InkwellSharedBoolean * _Nullable)richShowPageBackground richHeadingFont:(NSString * _Nullable)richHeadingFont richBodyFont:(NSString * _Nullable)richBodyFont richSharedFont:(NSString * _Nullable)richSharedFont paletteBackground:(NSString * _Nullable)paletteBackground paletteText:(NSString * _Nullable)paletteText paletteLink:(NSString * _Nullable)paletteLink paletteAccent:(NSString * _Nullable)paletteAccent paletteSurfaceHover:(NSString * _Nullable)paletteSurfaceHover basicBackground:(NSString * _Nullable)basicBackground basicForeground:(NSString * _Nullable)basicForeground basicAccent:(NSString * _Nullable)basicAccent basicAccentForeground:(NSString * _Nullable)basicAccentForeground __attribute__((swift_name("resolve(richBackgroundColor:richPageBackgroundColor:richPrimaryColor:richAccentBackgroundColor:richAccentTextColor:richPageWidth:richShowPageBackground:richHeadingFont:richBodyFont:richSharedFont:paletteBackground:paletteText:paletteLink:paletteAccent:paletteSurfaceHover:basicBackground:basicForeground:basicAccent:basicAccentForeground:)")));
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

__attribute__((swift_name("KotlinIterator")))
@protocol InkwellSharedKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
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
