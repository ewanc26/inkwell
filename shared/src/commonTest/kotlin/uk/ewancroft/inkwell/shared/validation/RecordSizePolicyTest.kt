package uk.ewancroft.inkwell.shared.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordSizePolicyTest {
    @Test
    fun acceptsRecordAtOrBelowLimit() {
        assertFalse(RecordSizePolicy.exceedsLimit(RecordSizePolicy.MAX_DOCUMENT_RECORD_BYTES))
        assertFalse(RecordSizePolicy.exceedsLimit(0))
    }

    @Test
    fun rejectsRecordAboveLimit() {
        assertTrue(RecordSizePolicy.exceedsLimit(RecordSizePolicy.MAX_DOCUMENT_RECORD_BYTES + 1))
    }

    @Test
    fun leavesTextContentUnderBudgetUntouched() {
        val text = "a".repeat(RecordSizePolicy.MAX_INLINE_TEXT_CONTENT_BYTES)
        assertEquals(text, RecordSizePolicy.truncateTextContent(text))
        assertNull(RecordSizePolicy.truncateTextContent(null))
    }

    @Test
    fun truncatesTextContentOverBudgetWithinByteLimit() {
        val text = "a".repeat(RecordSizePolicy.MAX_INLINE_TEXT_CONTENT_BYTES + 1000)
        val truncated = RecordSizePolicy.truncateTextContent(text)!!
        assertTrue(truncated.encodeToByteArray().size <= RecordSizePolicy.MAX_INLINE_TEXT_CONTENT_BYTES)
        assertTrue(truncated.endsWith(RecordSizePolicy.TEXT_CONTENT_TRUNCATION_SUFFIX))
    }

    @Test
    fun neverSplitsMultiByteScalars() {
        // Four-byte scalars: cutting mid-scalar would corrupt the string.
        val text = "😀".repeat(64)
        val truncated = RecordSizePolicy.truncateTextContent(text, limit = 40)!!
        assertTrue(truncated.encodeToByteArray().size <= 40)
        assertTrue(truncated.dropLast(1).all { it.isHighSurrogate() || it.isLowSurrogate() })
        assertEquals(0, truncated.dropLast(1).length % 2)
    }

    @Test
    fun returnsNullWhenBudgetCannotHoldAnything() {
        assertNull(RecordSizePolicy.truncateTextContent("hello", limit = 1))
        assertNull(RecordSizePolicy.truncateTextContent("hello", limit = 0))
    }
}
