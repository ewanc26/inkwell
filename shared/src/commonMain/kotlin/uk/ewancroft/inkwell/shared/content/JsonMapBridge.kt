package uk.ewancroft.inkwell.shared.content

import kotlinx.serialization.json.*

/**
 * Converts between generic Map-based content representations (used by shared
 * converters) and kotlinx.serialization JsonObjects (used by Android).
 *
 * This bridge lets the shared KMP converters remain JSON-library-agnostic
 * while the Android platform layer keeps its existing kotlinx.serialization
 * contract.
 */
object JsonMapBridge {

    /**
     * Recursively converts a [Map] tree to a [JsonObject].
     */
    fun mapToJson(map: Map<String, Any?>): JsonObject = buildJsonObject {
        for ((key, value) in map) {
            put(key, anyToJson(value))
        }
    }

    /**
     * Recursively converts a [JsonObject] to a [Map].
     */
    fun jsonToMap(obj: JsonObject): Map<String, Any?> = obj.mapValues { (_, value) ->
        jsonElementToAny(value)
    }

    private fun anyToJson(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            mapToJson(value as Map<String, Any?>)
        }
        is List<*> -> JsonArray(value.map { anyToJson(it) })
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }

    private fun jsonElementToAny(element: JsonElement): Any? = when (element) {
        is JsonObject -> jsonToMap(element)
        is JsonArray -> element.map { jsonElementToAny(it) }
        is JsonPrimitive -> {
            if (element.isString) element.content
            else {
                element.content.toIntOrNull()
                    ?: element.content.toLongOrNull()
                    ?: element.content.toDoubleOrNull()
                    ?: element.content.toBooleanStrictOrNull()
                    ?: element.content
            }
        }
    }
}
