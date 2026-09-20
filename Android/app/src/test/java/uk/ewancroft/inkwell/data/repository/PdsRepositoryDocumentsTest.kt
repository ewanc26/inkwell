package uk.ewancroft.inkwell.data.repository

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PdsRepositoryDocumentsTest {
    @Test
    fun `accepts an unknown or in-budget declared blob size`() {
        validateDeclaredBlobSize(null)
        validateDeclaredBlobSize(10L * 1024 * 1024)
    }

    @Test
    fun `rejects a declared blob size over the reader limit`() {
        assertFailsWith<IOException> {
            validateDeclaredBlobSize(10L * 1024 * 1024 + 1)
        }
    }
}
