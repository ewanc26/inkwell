package uk.ewancroft.inkwell.shared.validation

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Defensive limits for JSON received from untrusted PDSes. */
object JsonSafety {
    const val MAX_DEPTH = 32
    const val MAX_CONTAINER_ELEMENTS = 131_072
    const val MAX_STRING_BYTES = 1_048_576

    fun isSafe(root: JsonElement): Boolean {
        var containers = 0
        fun visit(element: JsonElement, depth: Int): Boolean {
            if (depth > MAX_DEPTH) return false
            when (element) {
                is JsonObject -> {
                    containers += element.size
                    if (containers > MAX_CONTAINER_ELEMENTS) return false
                    for ((key, value) in element) {
                        if (key.encodeToByteArray().size > MAX_STRING_BYTES || !visit(value, depth + 1)) return false
                    }
                }
                is JsonArray -> {
                    containers += element.size
                    if (containers > MAX_CONTAINER_ELEMENTS) return false
                    for (value in element) if (!visit(value, depth + 1)) return false
                }
                is JsonPrimitive -> if (element.content.encodeToByteArray().size > MAX_STRING_BYTES) return false
            }
            return true
        }
        return visit(root, 0)
    }
}
