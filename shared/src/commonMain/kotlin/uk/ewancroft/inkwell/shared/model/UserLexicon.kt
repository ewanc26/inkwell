package uk.ewancroft.inkwell.shared.model

import uk.ewancroft.inkwell.shared.graph.CollectionNsids

/**
 * Lexicon declaring that an account uses Inkwell.
 *
 * Record type: `uk.ewancroft.inkwell.user`
 *
 * Fields:
 *  - `user`: Boolean — true when the account holder uses Inkwell.
 *  - `app`: String (uri) — the canonical Inkwell app/website URI the record
 *    links to. Constellation indexes this link, so the network-wide set of
 *    Inkwell users can be enumerated by querying backlinks to [CANONICAL_APP_URI]
 *    with source [BACKLINK_SOURCE].
 *
 * The lexicon definition lives at `lexicons/uk.ewancroft.inkwell.user.json`.
 */
object UserLexicon {
    const val NSID = CollectionNsids.USER
    const val CANONICAL_APP_URI = "https://inkwell.ewancroft.uk"

    /** Constellation backlink source path for enumerating Inkwell-user records. */
    const val BACKLINK_SOURCE = "$NSID:app"
}

data class UserLexiconRecord(
    val user: Boolean,
    val app: String = UserLexicon.CANONICAL_APP_URI,
    val type: String = UserLexicon.NSID,
)

/** Utility functions for [UserLexiconRecord]. */
object UserLexiconUtils {

    /** Creates an Inkwell-user declaration for the signed-in account. */
    fun createForUser(user: Boolean = true): UserLexiconRecord =
        UserLexiconRecord(user = user)

    /** Returns true if the given `$type` string represents a UserLexicon record. */
    fun isUserLexiconType(type: String?): Boolean =
        type == UserLexicon.NSID
}
