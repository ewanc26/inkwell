package uk.ewancroft.inkwell.ui.writer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayOutputStream

/** Decodes and re-encodes user images without carrying source metadata. */
object ImageUploadSanitizer {
    private const val MAX_DIMENSION = 8_192
    private const val MAX_PIXELS = 40_000_000L

    data class Output(val bytes: ByteArray, val mimeType: String)

    fun sanitize(bytes: ByteArray): Output {
        require(!isAnimatedGif(bytes)) {
            "Animated images are not supported for upload. Choose a still image."
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The selected file is not a supported image." }
        require(bounds.outWidth <= MAX_DIMENSION && bounds.outHeight <= MAX_DIMENSION) {
            "That image is too large to upload safely."
        }
        require(bounds.outWidth.toLong() * bounds.outHeight <= MAX_PIXELS) {
            "That image is too large to upload safely."
        }

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("The selected file is not a supported image.")
        val oriented = applyOrientation(bytes, decoded)
        val format = if (oriented.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        val output = ByteArrayOutputStream()
        check(oriented.compress(format, 90, output)) { "The image could not be prepared for upload." }
        if (oriented !== decoded) decoded.recycle()
        oriented.recycle()
        return Output(output.toByteArray(), mimeType)
    }

    private fun isAnimatedGif(bytes: ByteArray): Boolean {
        if (bytes.size < 13 || bytes[0] != 'G'.code.toByte() || bytes[1] != 'I'.code.toByte() ||
            bytes[2] != 'F'.code.toByte() || bytes[3] != '8'.code.toByte() ||
            (bytes[4] != '7'.code.toByte() && bytes[4] != '9'.code.toByte()) ||
            bytes[5] != 'a'.code.toByte()) return false

        var offset = 13
        val packed = bytes[10].toInt() and 0xff
        if (packed and 0x80 != 0) offset += 3 * (1 shl ((packed and 0x07) + 1))
        if (offset > bytes.size) return false

        while (offset < bytes.size) {
            when (bytes[offset].toInt() and 0xff) {
                0x21 -> {
                    if (offset + 1 >= bytes.size) return false
                    if ((bytes[offset + 1].toInt() and 0xff) == 0xf9) return true
                    offset += 2
                    while (offset < bytes.size) {
                        val length = bytes[offset].toInt() and 0xff
                        offset += 1 + length
                        if (length == 0) break
                    }
                }
                0x2c -> return false
                0x3b -> return false
                else -> return false
            }
        }
        return false
    }

    private fun applyOrientation(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(bytes.inputStream()).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
