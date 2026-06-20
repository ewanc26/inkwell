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
    /// Inkwell currently authenticates with an app password via `ATProtocolConfiguration`
    /// (see `LoginStateManager`) rather than OAuth, so these aren't wired into the sign-in
    /// flow yet — app-password sessions get whatever access the account itself has, with no
    /// scoping. They're here for whenever OAuth (and therefore properly scoped, user-consented
    /// permissions) lands.
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
