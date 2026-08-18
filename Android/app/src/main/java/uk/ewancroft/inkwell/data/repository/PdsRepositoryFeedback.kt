package uk.ewancroft.inkwell.data.repository

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import uk.ewancroft.inkwell.shared.feedback.UserInputLexicon
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import java.time.Instant

suspend fun PdsRepository.createPublication(
    url: String,
    name: String,
    description: String? = null,
): JsonObject {
    val record = buildJsonObject {
        put("\$type", CollectionNsids.PUBLICATION)
        put("url", url)
        put("name", name)
        if (description != null) put("description", description)
    }
    return createRecord(CollectionNsids.PUBLICATION, record)
}

// ── Feedback (app.userinput.discussion) ─────────────────────────────
//
// Sends in-app feedback to Inkwell's userinput.app board (owned by
// ewancroft.uk). The discussion record lives in the *submitting user's*
// own repo — same repo-ownership model as site.standard.graph.recommend
// — and points at Inkwell's fixed feedback space via a strong
// reference. Only creation is implemented; Inkwell doesn't read or
// moderate the board.

/**
 * Posts feedback to Inkwell's userinput.app space. [tag], if provided,
 * must be one of [UserInputLexicon.TAGS].
 */
suspend fun PdsRepository.submitFeedback(title: String, body: String?, tag: String? = null): JsonObject {
    require(title.isNotBlank()) { "title must not be blank" }
    require(title.length <= UserInputLexicon.TITLE_MAX_LENGTH) { "title exceeds max length" }
    require(body == null || body.length <= UserInputLexicon.BODY_MAX_LENGTH) { "body exceeds max length" }
    require(tag == null || tag in UserInputLexicon.TAGS) { "unknown tag: $tag" }

    // Resolve the space's current CID — a strongRef must match exactly.
    val space = getRecord(UserInputLexicon.INKWELL_SPACE_URI)
    val spaceCid = space["cid"]?.jsonPrimitive?.content
        ?: throw IllegalStateException("Could not resolve Inkwell's feedback space")

    val record = buildJsonObject {
        put("\$type", UserInputLexicon.DISCUSSION)
        put("title", title)
        if (!body.isNullOrBlank()) put("body", body)
        if (tag != null) putJsonArray("tags") { add(tag) }
        putJsonObject("space") {
            put("uri", UserInputLexicon.INKWELL_SPACE_URI)
            put("cid", spaceCid)
        }
        put("createdAt", Instant.now().toString())
    }
    return createRecord(UserInputLexicon.DISCUSSION, record)
}
