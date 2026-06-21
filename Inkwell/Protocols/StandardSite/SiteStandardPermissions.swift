//
//  SiteStandardPermissions.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//

import Foundation

extension SiteStandardLexicon {

    /// Standard.site's OAuth permission sets.
    ///
    /// These aren't lexicon records — they're scope identifiers a client includes when
    /// requesting authorization, so the user's PDS knows which `site.standard.*` collections
    /// the app is allowed to create/update/delete records in.
    ///
    /// Inkwell authenticates via OAuth (using OAuthenticator — see `LoginStateManager`).
    /// These scope identifiers are included in the OAuth authorization request so the user's
    /// PDS knows which `site.standard.*` collections the app is allowed to access.
    /// Currently Inkwell requests the `atproto` scope which grants full repository access;
    /// finer-grained site.standard.* scopes may be adopted in the future.
    ///
    /// - SeeAlso: [standard.site/docs/permissions](https://standard.site/docs/permissions/)
    public enum Permissions {

        /// Full access to publications, documents, subscriptions, and recommends.
        ///
        /// Grants create/update/delete on:
        /// `site.standard.publication`, `site.standard.document`,
        /// `site.standard.graph.subscription`, `site.standard.graph.recommend`.
        ///
        /// Requested via OAuth scope as `include:site.standard.authFull`.
        public static let authFull = "site.standard.authFull"

        /// A narrower scope for managing subscriptions and document recommends only.
        ///
        /// Grants create/update/delete on:
        /// `site.standard.graph.subscription`, `site.standard.graph.recommend`.
        ///
        /// Requested via OAuth scope as `include:site.standard.authSocial`. Appropriate for
        /// reader-only features (subscribing to publications, recommending documents) that
        /// shouldn't also be able to touch someone's actual publication/document records.
        public static let authSocial = "site.standard.authSocial"
    }
}
