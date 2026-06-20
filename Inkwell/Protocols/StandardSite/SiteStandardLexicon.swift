//
//  SiteStandardLexicon.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//

import Foundation

/// The root namespace for Standard.site (`site.standard.*`) lexicons.
///
/// [Standard.site](https://standard.site) is a community-maintained set of AT Protocol
/// lexicons for long-form publishing — "one schema, every platform." It's what Leaflet (and
/// Inkwell's eventual leaflet.pub support) publishes documents and publications as, alongside
/// other tools like pckt.blog, offprint.app, and self-hosted sites via sequoia.pub.
///
/// These types deliberately follow the same modelling conventions ATProtoKit itself uses for
/// Bluesky's `app.bsky.*`/`com.atproto.*` lexicons (see `AppBskyLexicon`/`ComAtprotoLexicon`),
/// so they slot into the same `ATRecordProtocol`/`UnknownType` machinery rather than being a
/// parallel, bespoke decoding path:
/// - Top-level collection records (the ones actually stored at a repo path) conform to
///   `ATRecordProtocol`, exactly like `AppBskyLexicon.Feed.PostRecord` etc.
/// - Shared AT Protocol primitives (blobs, strong refs, self-labels) reuse ATProtoKit's own
///   `ComAtprotoLexicon` types instead of redefining them.
/// - Open-union fields (content formats Standard.site deliberately leaves up to each platform)
///   use ATProtoKit's `UnknownType`.
///
/// Call ``SiteStandardLexicon/registerRecordTypes()`` once at launch (see
/// `SiteStandardRegistration.swift`) so those open unions can resolve `site.standard.*`
/// content by its `$type` instead of falling back to raw JSON.
///
/// Four lexicons make up the standard:
/// - ``SiteStandardLexicon/PublicationRecord`` — `site.standard.publication`
/// - ``SiteStandardLexicon/DocumentRecord`` — `site.standard.document`
/// - ``SiteStandardLexicon/Graph/SubscriptionRecord`` — `site.standard.graph.subscription`
/// - ``SiteStandardLexicon/Graph/RecommendRecord`` — `site.standard.graph.recommend`
///
/// Plus two supporting pieces: ``SiteStandardLexicon/Theme`` for publication theming, and
/// ``SiteStandardLexicon/Verification`` for proving a record actually belongs to the domain
/// it claims to.
///
/// - Note: Standard.site is explicitly *not* a standard for site content — only metadata,
/// discovery, and social features. Don't expect (or add) a Leaflet-specific block-content
/// model here; that belongs in its own lexicon, registered separately if/when Inkwell needs
/// to decode it.
///
/// - SeeAlso: [standard.site/docs/introduction](https://standard.site/docs/introduction/)
public enum SiteStandardLexicon {

    /// Namespace for `site.standard.graph.*` lexicons (subscriptions, recommends).
    public enum Graph {}

    /// Namespace for `site.standard.theme.*` lexicons (publication theming).
    public enum Theme {}
}
