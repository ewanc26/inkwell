package uk.ewancroft.inkwell.ui.writer

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream

internal const val MAX_IMAGE_UPLOAD_BYTES = 10 * 1024 * 1024

/** Outcome of reading a picked image, before sanitizing. */
internal sealed interface PickedImageBytes {
    data class Read(val bytes: ByteArray) : PickedImageBytes
    data object TooLarge : PickedImageBytes
    data object Unreadable : PickedImageBytes
}

/**
 * Reads a picked image, stopping just past [MAX_IMAGE_UPLOAD_BYTES] so an
 * oversized file is rejected without being buffered whole.
 *
 * Shared by the inline-image and cover-image pickers so both enforce the same cap.
 */
internal fun readPickedImage(context: Context, uri: Uri): PickedImageBytes {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            output.write(buffer, 0, read)
            if (total > MAX_IMAGE_UPLOAD_BYTES) break
        }
        output.toByteArray()
    } ?: return PickedImageBytes.Unreadable
    return if (bytes.size > MAX_IMAGE_UPLOAD_BYTES) PickedImageBytes.TooLarge else PickedImageBytes.Read(bytes)
}
