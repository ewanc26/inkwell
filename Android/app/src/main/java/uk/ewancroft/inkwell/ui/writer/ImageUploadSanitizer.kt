package uk.ewancroft.inkwell.ui.writer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/** Decodes and re-encodes user images without carrying source metadata. */
object ImageUploadSanitizer {
    private const val MAX_INPUT_BYTES = 10 * 1024 * 1024
    private const val MAX_DIMENSION = 8_192
    private const val MAX_PIXELS = 40_000_000L

    private const val DOWNSCALE_FACTOR = 0.75f
    private const val MAX_DOWNSCALE_STEPS = 8

    data class Output(val bytes: ByteArray, val mimeType: String)

    /**
     * @param maxOutputBytes when set, the re-encoded image is progressively
     *   downscaled until it fits — used for fields with a Lexicon `maxSize`,
     *   such as a document's cover image.
     */
    fun sanitize(bytes: ByteArray, maxOutputBytes: Int? = null): Output {
        require(bytes.size <= MAX_INPUT_BYTES) { "That image file is too large to process safely." }
        require(!GifAnimationDetector.isAnimated(bytes)) {
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
        val preserveAlpha = containsTransparency(decoded)
        val oriented = applyOrientation(bytes, decoded)
        val format = if (preserveAlpha) {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }
        val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        if (oriented !== decoded) decoded.recycle()
        val encoded = encode(oriented, format)
        val fitted = if (maxOutputBytes == null) encoded else fitWithin(oriented, format, encoded, maxOutputBytes)
        oriented.recycle()
        return Output(fitted, mimeType)
    }

    private fun encode(bitmap: Bitmap, format: Bitmap.CompressFormat): ByteArray {
        val output = ByteArrayOutputStream()
        check(bitmap.compress(format, 90, output)) { "The image could not be prepared for upload." }
        return output.toByteArray()
    }

    /** Rescales from [source] each step so quality loss does not compound. */
    private fun fitWithin(
        source: Bitmap,
        format: Bitmap.CompressFormat,
        initial: ByteArray,
        maxBytes: Int,
    ): ByteArray {
        var encoded = initial
        var scale = 1f
        repeat(MAX_DOWNSCALE_STEPS) {
            if (encoded.size <= maxBytes) return encoded
            scale *= DOWNSCALE_FACTOR
            val scaled = Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true,
            )
            encoded = encode(scaled, format)
            if (scaled !== source) scaled.recycle()
        }
        require(encoded.size <= maxBytes) { "That image could not be made small enough. Choose a simpler image." }
        return encoded
    }

    private fun containsTransparency(bitmap: Bitmap): Boolean {
        if (bitmap.config != Bitmap.Config.ARGB_8888) return false
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            if (row.any { (it ushr 24) != 0xFF }) return true
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
