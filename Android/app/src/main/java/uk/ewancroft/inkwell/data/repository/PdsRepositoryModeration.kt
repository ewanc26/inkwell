package uk.ewancroft.inkwell.data.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import uk.ewancroft.inkwell.TestingConfig
import uk.ewancroft.inkwell.TestingModeException
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import java.time.Instant

// ── Graph: Mutes & Blocks (app.bsky.graph.*) ────────────────────────────
//
// Mutes are server-side (no public record — app.bsky.graph.muteActor /
// unmuteActor / getMutes). Blocks are a record in the signed-in user's
// own repo (app.bsky.graph.block, created/deleted via the same
// createRecord/deleteRecord path used by subscriptions/recommends), with
// app.bsky.graph.getBlocks used to enrich the resulting DIDs with a
// handle/display name for the management UI.

private const val BLUESKY_APPVIEW_PROXY = "did:web:api.bsky.app#bsky_appview"

data class ModeratedActor(val did: String, val handle: String, val displayName: String? = null)

data class BlockedActorEntry(val actor: ModeratedActor, val rkey: String)

@Serializable
private data class ActorInput(val actor: String)

@Serializable
private data class ListParams(val limit: Int = 100, val cursor: String? = null)

private fun parseActor(json: JsonObject): ModeratedActor? {
    val did = json["did"]?.jsonPrimitive?.contentOrNull ?: return null
    val handle = json["handle"]?.jsonPrimitive?.contentOrNull ?: did
    val displayName = json["displayName"]?.jsonPrimitive?.contentOrNull
    return ModeratedActor(did, handle, displayName)
}

/** Mutes [did] via `app.bsky.graph.muteActor` (server-side; no public record). */
suspend fun PdsRepository.muteActor(did: String) {
    if (TestingConfig.enabled) {
        TestingConfig.report("Mute actor")
        throw TestingModeException("Mute actor")
    }
    sessionStore.load() ?: throw Exception("Not authenticated")
    val authClient = atOAuth.createClient()
    authClient.procedure(
        nsid = "app.bsky.graph.muteActor",
        params = Unit,
        paramsSerializer = Unit.serializer(),
        input = ActorInput(did),
        inputSerializer = ActorInput.serializer(),
        responseSerializer = Unit.serializer(),
        proxy = BLUESKY_APPVIEW_PROXY,
    )
}

/** Unmutes [did] via `app.bsky.graph.unmuteActor`. */
suspend fun PdsRepository.unmuteActor(did: String) {
    if (TestingConfig.enabled) {
        TestingConfig.report("Unmute actor")
        throw TestingModeException("Unmute actor")
    }
    sessionStore.load() ?: throw Exception("Not authenticated")
    val authClient = atOAuth.createClient()
    authClient.procedure(
        nsid = "app.bsky.graph.unmuteActor",
        params = Unit,
        paramsSerializer = Unit.serializer(),
        input = ActorInput(did),
        inputSerializer = ActorInput.serializer(),
        responseSerializer = Unit.serializer(),
        proxy = BLUESKY_APPVIEW_PROXY,
    )
}

/** Lists the signed-in user's muted actors via `app.bsky.graph.getMutes` (paginated to completion). */
suspend fun PdsRepository.fetchMutedActors(): List<ModeratedActor> {
    sessionStore.load() ?: throw Exception("Not authenticated")
    val authClient = atOAuth.createClient()
    val all = mutableListOf<ModeratedActor>()
    var cursor: String? = null
    do {
        val response = authClient.query(
            nsid = "app.bsky.graph.getMutes",
            params = ListParams(cursor = cursor),
            paramsSerializer = ListParams.serializer(),
            responseSerializer = JsonObject.serializer(),
            proxy = BLUESKY_APPVIEW_PROXY,
        )
        val mutes = response["mutes"]?.jsonArray.orEmpty()
        mutes.forEach { parseActor(it.jsonObject)?.let(all::add) }
        val next = response["cursor"]?.jsonPrimitive?.contentOrNull
        if (next == null || next == cursor || mutes.isEmpty()) break
        cursor = next
    } while (true)
    return all
}

/** Creates an `app.bsky.graph.block` record: blocks [did]. */
suspend fun PdsRepository.createBlock(did: String): String {
    val record = buildJsonObject {
        put("\$type", CollectionNsids.GRAPH_BLOCK)
        put("subject", did)
        put("createdAt", Instant.now().toString())
    }
    val result = createRecord(CollectionNsids.GRAPH_BLOCK, record)
    val uri = result["uri"]?.jsonPrimitive?.content ?: throw Exception("createRecord returned no uri")
    return AtUri.parse(uri)?.recordKey ?: throw Exception("Invalid AT-URI returned: $uri")
}

/** Deletes a block record by its record key. */
suspend fun PdsRepository.deleteBlock(rkey: String) = deleteRecord(CollectionNsids.GRAPH_BLOCK, rkey)

/**
 * Lists the signed-in user's blocked actors: enumerates the user's own
 * `app.bsky.graph.block` records (for rkeys, used to unblock) and enriches
 * them with handle/display name via `app.bsky.graph.getBlocks`.
 */
suspend fun PdsRepository.fetchBlockedActors(did: String): List<BlockedActorEntry> {
    val ownRecords = listAllRecords(did, CollectionNsids.GRAPH_BLOCK)
    val rkeyBySubject = ownRecords.mapNotNull { entry ->
        val subject = entry.value["subject"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val rkey = AtUri.parse(entry.uri)?.recordKey ?: return@mapNotNull null
        subject to rkey
    }.toMap()
    if (rkeyBySubject.isEmpty()) return emptyList()

    sessionStore.load() ?: throw Exception("Not authenticated")
    val authClient = atOAuth.createClient()
    val actorsByDid = mutableMapOf<String, ModeratedActor>()
    var cursor: String? = null
    do {
        val response = authClient.query(
            nsid = "app.bsky.graph.getBlocks",
            params = ListParams(cursor = cursor),
            paramsSerializer = ListParams.serializer(),
            responseSerializer = JsonObject.serializer(),
            proxy = BLUESKY_APPVIEW_PROXY,
        )
        val blocks = response["blocks"]?.jsonArray.orEmpty()
        blocks.forEach { parseActor(it.jsonObject)?.let { actor -> actorsByDid[actor.did] = actor } }
        val next = response["cursor"]?.jsonPrimitive?.contentOrNull
        if (next == null || next == cursor || blocks.isEmpty()) break
        cursor = next
    } while (true)

    return rkeyBySubject.map { (subjectDid, rkey) ->
        val actor = actorsByDid[subjectDid] ?: ModeratedActor(did = subjectDid, handle = subjectDid)
        BlockedActorEntry(actor, rkey)
    }
}
