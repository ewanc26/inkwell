package uk.ewancroft.inkwell.ui.writer

/** Detects multi-frame GIFs without decoding their pixel payload. */
internal object GifAnimationDetector {
    fun isAnimated(bytes: ByteArray): Boolean {
        if (bytes.size < 13 || bytes[0] != 'G'.code.toByte() || bytes[1] != 'I'.code.toByte() ||
            bytes[2] != 'F'.code.toByte() || bytes[3] != '8'.code.toByte() ||
            (bytes[4] != '7'.code.toByte() && bytes[4] != '9'.code.toByte()) ||
            bytes[5] != 'a'.code.toByte()) return false

        var offset = 13
        val packed = bytes[10].toInt() and 0xff
        if (packed and 0x80 != 0) offset += 3 * (1 shl ((packed and 0x07) + 1))
        if (offset > bytes.size) return false

        var imageCount = 0
        while (offset < bytes.size) {
            when (bytes[offset].toInt() and 0xff) {
                0x21 -> {
                    if (offset + 1 >= bytes.size) return false
                    offset += 2
                    while (offset < bytes.size) {
                        val length = bytes[offset].toInt() and 0xff
                        offset += 1 + length
                        if (length == 0) break
                    }
                }
                0x2c -> {
                    if (++imageCount > 1) return true
                    if (offset + 9 >= bytes.size) return false
                    val imagePacked = bytes[offset + 9].toInt() and 0xff
                    offset += 10
                    if (imagePacked and 0x80 != 0) {
                        offset += 3 * (1 shl ((imagePacked and 0x07) + 1))
                    }
                    if (offset >= bytes.size) return false
                    offset++
                    while (offset < bytes.size) {
                        val length = bytes[offset].toInt() and 0xff
                        offset += 1 + length
                        if (length == 0) break
                    }
                }
                0x3b -> return false
                else -> return false
            }
        }
        return false
    }
}
