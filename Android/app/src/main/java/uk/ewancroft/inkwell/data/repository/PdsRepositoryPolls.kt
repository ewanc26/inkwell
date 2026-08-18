package uk.ewancroft.inkwell.data.repository

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import uk.ewancroft.inkwell.data.model.content.LeafletPollDefinition
import uk.ewancroft.inkwell.data.model.content.LeafletPollVote
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.xrpc.XrpcEndpoints

suspend fun PdsRepository.getPollDefinition(did: String, rkey: String): LeafletPollDefinition {
    val urlStr = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.REPO_GET_RECORD}?repo=${enc(did)}&collection=${CollectionNsids.LEAFLET_POLL_DEFINITION}&rkey=${enc(rkey)}"
    val body = executeGet(urlStr)
    val record = json.parseToJsonElement(body).jsonObject
    val value = record["value"]?.jsonObject ?: throw IllegalStateException("Missing poll value")
    return json.decodeFromJsonElement(LeafletPollDefinition.serializer(), value)
}

suspend fun PdsRepository.listPollVotes(did: String, pollRkey: String): List<LeafletPollVote> {
    val urlStr = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.REPO_LIST_RECORDS}?repo=${enc(did)}&collection=${CollectionNsids.LEAFLET_POLL_VOTE}&limit=100"
    val body = executeGet(urlStr)
    val response = json.parseToJsonElement(body).jsonObject
    val records = response["records"]?.jsonArray.orEmpty()
    return records.mapNotNull { record ->
        val uri = record.jsonObject["uri"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val value = record.jsonObject["value"]?.jsonObject ?: return@mapNotNull null
        val vote = runCatching { json.decodeFromJsonElement(LeafletPollVote.serializer(), value) }.getOrNull()
            ?: return@mapNotNull null
        val rkey = AtUri.parse(uri)?.recordKey ?: return@mapNotNull null
        if (rkey != pollRkey) return@mapNotNull null
        vote
    }
}

suspend fun PdsRepository.createPollVote(did: String, pollUri: String, options: List<String>): JsonObject {
    val session = sessionStore.load() ?: throw Exception("Not authenticated")
    val authClient = atOAuth.createClient()
    return authClient.procedure(
        nsid = "com.atproto.repo.createRecord",
        params = Unit,
        paramsSerializer = Unit.serializer(),
        input = buildJsonObject {
            put("repo", session.did)
            put("collection", CollectionNsids.LEAFLET_POLL_VOTE)
            put("record", buildJsonObject {
                put("\$type", CollectionNsids.LEAFLET_POLL_VOTE)
                put("poll", buildJsonObject {
                    put("uri", pollUri)
                })
                put("option", buildJsonArray { options.forEach { add(JsonPrimitive(it)) } })
            })
        },
        inputSerializer = JsonObject.serializer(),
        responseSerializer = JsonObject.serializer(),
    )
}
