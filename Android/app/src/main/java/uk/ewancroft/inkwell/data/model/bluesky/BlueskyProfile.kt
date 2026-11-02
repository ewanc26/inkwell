/**
 * Bluesky profile and Constellation backlink shapes.
 *
 * Profiles come from the public Bluesky API (app.bsky.actor.getProfile).
 * Backlinks come from microcosm.blue's Constellation index and are the
 * core cross-repo discovery mechanism — they tell us which records across
 * the entire AT Protocol network reference a given document.
 */
package uk.ewancroft.inkwell.data.model.bluesky

import kotlinx.serialization.Serializable

// ── Bluesky Profile ──────────────────────────────────────────────────────

/** Public profile data returned by the Bluesky API. */
@Serializable
data class BlueskyProfile(
    val did: String,
    val handle: String,
    val displayName: String? = null,
    val pronouns: String? = null,
    val description: String? = null,
    val avatar: String? = null,
    val banner: String? = null,
    val followersCount: Int? = null,
    val followsCount: Int? = null,
    val postsCount: Int? = null,
    val links: List<BlueskyProfileLink>? = null,
    val verification: BSkyVerificationState? = null,
    val pinnedPost: BSkyStrongRef? = null
)

/** `true` when the AppView reports a valid account verification. */
fun BlueskyProfile.isVerifiedAccount(): Boolean =
    verification?.verifiedStatus == BSkyVerificationState.STATUS_VALID

/**
 * `app.bsky.actor.defs#verificationState` — only the account-level status
 * is surfaced; individual verifications are not rendered.
 */
@Serializable
data class BSkyVerificationState(
    val verifiedStatus: String? = null,
    val trustedVerifierStatus: String? = null,
) {
    companion object {
        const val STATUS_VALID = "valid"
    }
}

/**
 * A single `app.bsky.actor.defs#profileLinkView` — one of the (up to three)
 * links a user pins to their Bluesky profile. Only `profileViewDetailed`
 * (i.e. `app.bsky.actor.getProfile`) carries these; list subjects decode as
 * `profileView` and simply leave [BlueskyProfile.links] null.
 */
@Serializable
data class BlueskyProfileLink(
    val uri: String? = null,
    val url: String? = null,
    val title: String? = null,
    val icon: String? = null
)

// ── Bluesky List (app.bsky.graph.getList) ────────────────────────────────

/** A single member of a Bluesky list — the subject's profile view. */
@Serializable
data class BlueskyListItem(
    val subject: BlueskyProfile
)

/** Response from `app.bsky.graph.getList`. */
@Serializable
data class GetListResponse(
    val items: List<BlueskyListItem> = emptyList(),
    val cursor: String? = null
)

// ── Constellation Backlinks ──────────────────────────────────────────────

/**
 * A single backlink record from Constellation: a (did, collection, rkey)
 * tuple identifying a record somewhere on the network that links to a
 * document of interest.
 */
@Serializable
data class ConstellationBacklink(
    val did: String,
    val collection: String,
    val rkey: String
) {
    /** Synthetic AT-URI assembled from the three components. */
    val recordUri: String get() = "at://$did/$collection/$rkey"
}

/** Paginated response container from the Constellation API. */
@Serializable
data class ConstellationResponse(
    val records: List<ConstellationBacklink>? = null,
    val cursor: String? = null
)
