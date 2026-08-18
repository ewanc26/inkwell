package uk.ewancroft.inkwell.shared.feedback

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
object UserInputLexicon {
    /** A feedback post, created in the submitting user's own repo. */
    const val DISCUSSION = "app.userinput.discussion"

    /** A feedback board/space. Inkwell only reads its own (see [INKWELL_SPACE_URI]). */
    const val SPACE = "app.userinput.space"

    /** Inkwell's own feedback space, owned by ewancroft.uk. */
    const val INKWELL_SPACE_URI = "at://did:plc:ofrbh253gwicbkc5nktqepol/app.userinput.space/3mtdoxmi2lp27"

    /** Tag values defined on Inkwell's feedback space, in display order. */
    val TAGS = listOf("bug", "question", "ios", "android", "altstore", "f-droid")

    /** `app.userinput.discussion.title` — maximum length in UTF-16 code units. */
    const val TITLE_MAX_LENGTH = 600

    /** `app.userinput.discussion.title` — maximum length in Unicode grapheme clusters. */
    const val TITLE_MAX_GRAPHEMES = 300

    /** `app.userinput.discussion.body` — maximum length in UTF-16 code units. */
    const val BODY_MAX_LENGTH = 20_000

    /** `app.userinput.discussion.body` — maximum length in Unicode grapheme clusters. */
    const val BODY_MAX_GRAPHEMES = 10_000

    /** Maximum number of tags a discussion may carry. */
    const val MAX_TAGS = 8
}
