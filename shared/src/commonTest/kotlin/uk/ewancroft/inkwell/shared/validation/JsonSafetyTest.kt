package uk.ewancroft.inkwell.shared.validation

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
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
}
