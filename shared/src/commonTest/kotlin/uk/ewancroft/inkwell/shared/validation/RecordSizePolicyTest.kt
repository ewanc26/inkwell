package uk.ewancroft.inkwell.shared.validation

import kotlin.test.Test
import kotlin.test.assertFalse
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
}
