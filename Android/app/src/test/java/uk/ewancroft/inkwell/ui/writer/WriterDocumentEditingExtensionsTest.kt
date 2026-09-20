package uk.ewancroft.inkwell.ui.writer

import org.junit.Assert.assertEquals
import org.junit.Test

class WriterDocumentEditingExtensionsTest {
    @Test
    fun invalidSwapIsReportedAsReloadConflict() {
        assertEquals(
            "This document changed elsewhere. Reload it before deleting.",
            deleteDocumentErrorMessage(IllegalStateException("InvalidSwap")),
        )
    }

    @Test
    fun cancellationOrOtherFailureKeepsGenericError() {
        assertEquals(
            "Failed to delete document: cancelled",
            deleteDocumentErrorMessage(IllegalStateException("cancelled")),
        )
    }
}
