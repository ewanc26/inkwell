package uk.ewancroft.inkwell.shared.oauth

object OAuthScopes {
    const val ATPROTO = "atproto"
    const val BLOB_ALL = "blob:*/*"
    const val REPO_PUBLICATION = "repo:site.standard.publication"
    const val REPO_DOCUMENT = "repo:site.standard.document"
    /** Needed to post feedback to Inkwell's userinput.app board. */
    const val REPO_USERINPUT_DISCUSSION = "repo:app.userinput.discussion"
    const val REPO_SUBSCRIPTION = "repo:site.standard.graph.subscription"
    const val REPO_RECOMMEND = "repo:site.standard.graph.recommend"
    const val AUTH_FULL = "site.standard.authFull"
    const val AUTH_SOCIAL = "site.standard.authSocial"
    /** Scope for moderation RPCs (report, etc.) proxied through the Bluesky AppView. */
    const val MODERATION = "moderation"
    /** Scope for the Inkwell-user declaration record write. */
    const val REPO_USER = "repo:uk.ewancroft.inkwell.user"
}
