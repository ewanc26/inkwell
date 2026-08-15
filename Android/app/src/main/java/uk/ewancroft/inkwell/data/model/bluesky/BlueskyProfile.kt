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
    val description: String? = null,
    val avatar: String? = null,
    val banner: String? = null,
    val followersCount: Int? = null,
    val followsCount: Int? = null,
    val postsCount: Int? = null
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
