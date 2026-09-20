package uk.ewancroft.inkwell.ui.writer

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageUploadSanitizerTest {
    @Test
    fun `rejects oversized input before image decoding`() {
        assertFailsWith<IllegalArgumentException> {
            ImageUploadSanitizer.sanitize(ByteArray(10 * 1024 * 1024 + 1))
        }
    }

    @Test
    fun `reencoded JPEG has matching signature and MIME`() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.RGB_565).apply {
            eraseColor(0xFFFF0000.toInt())
        }
        val encoded = ByteArrayOutputStream().also { output ->
            source.compress(Bitmap.CompressFormat.PNG, 100, output)
            source.recycle()
        }.toByteArray()

        val result = ImageUploadSanitizer.sanitize(encoded)

        assertEquals("image/jpeg", result.mimeType)
        assertContentEquals(
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
            result.bytes.copyOf(3),
        )
    }
}
