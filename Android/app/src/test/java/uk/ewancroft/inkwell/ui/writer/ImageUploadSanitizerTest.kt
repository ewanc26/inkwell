package uk.ewancroft.inkwell.ui.writer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.exifinterface.media.ExifInterface
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

    @Test
    fun `transparent input is reencoded as PNG with matching MIME`() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0x0000FF00)
        }
        val encoded = ByteArrayOutputStream().also { output ->
            source.compress(Bitmap.CompressFormat.PNG, 100, output)
            source.recycle()
        }.toByteArray()

        val result = ImageUploadSanitizer.sanitize(encoded)

        assertEquals("image/png", result.mimeType)
        assertContentEquals(
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47),
            result.bytes.copyOf(4),
        )
    }

    @Test
    fun `EXIF orientation is applied before metadata is stripped`() {
        val source = Bitmap.createBitmap(2, 3, Bitmap.Config.RGB_565).apply {
            eraseColor(0xFFFF0000.toInt())
        }
        val file = File.createTempFile("inkwell-orientation-", ".jpg")
        try {
            ByteArrayOutputStream().also { output ->
                source.compress(Bitmap.CompressFormat.JPEG, 100, output)
                file.writeBytes(output.toByteArray())
                source.recycle()
            }
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }

            val result = ImageUploadSanitizer.sanitize(file.readBytes())
            val decoded = BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size)

            assertEquals(3, decoded.width)
            assertEquals(2, decoded.height)
            decoded.recycle()
        } finally {
            file.delete()
        }
    }
}
