package uk.ewancroft.inkwell.shared.validation

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonSafetyTest {
    @Test
    fun rejectsExcessiveNesting() {
        var value = buildJsonObject { put("leaf", true) }
        repeat(JsonSafety.MAX_DEPTH + 1) { value = buildJsonObject { put("child", value) } }
        assertFalse(JsonSafety.isSafe(value))
    }

    @Test
    fun acceptsNormalRecords() {
        val value = buildJsonArray { add(buildJsonObject { put("title", "hello") }) }
        assertTrue(JsonSafety.isSafe(value))
    }

    @Test
    fun rejectsTooManyContainerElements() {
        val value = buildJsonArray {
            repeat(JsonSafety.MAX_CONTAINER_ELEMENTS + 1) { add(JsonPrimitive(true)) }
        }
        assertFalse(JsonSafety.isSafe(value))
    }

    @Test
    fun rejectsOversizedPrimitiveContent() {
        val value = JsonPrimitive("x".repeat(JsonSafety.MAX_STRING_BYTES + 1))
        assertFalse(JsonSafety.isSafe(value))
    }

    @Test
    fun rejectsMultibytePrimitiveContentOverByteLimit() {
        val value = JsonPrimitive("é".repeat(JsonSafety.MAX_STRING_BYTES / 2 + 1))
        assertFalse(JsonSafety.isSafe(value))
    }

    @Test
    fun rejectsOversizedObjectKeys() {
        val value = buildJsonObject {
            put("x".repeat(JsonSafety.MAX_STRING_BYTES + 1), true)
        }
        assertFalse(JsonSafety.isSafe(value))
    }

    @Test
    fun acceptsContainerAndStringLimitsExactly() {
        val value = buildJsonObject {
            repeat(JsonSafety.MAX_CONTAINER_ELEMENTS) { put("key$it", true) }
        }
        assertTrue(JsonSafety.isSafe(value))
        assertTrue(JsonSafety.isSafe(JsonPrimitive("x".repeat(JsonSafety.MAX_STRING_BYTES))))
    }
}
