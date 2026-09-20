package uk.ewancroft.inkwell.ui.writer

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GifAnimationDetectorTest {
    @Test
    fun `recognizes a still GIF`() {
        val still = "R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==".decodeBase64()

        assertFalse(GifAnimationDetector.isAnimated(still))
    }

    @Test
    fun `recognizes a two-frame GIF`() {
        val animated = "R0lGODlhAQABAPAAAP8AAAAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQACgAAACwAAAAAAQABAAACAkQBACH5BAAKAAAALAAAAAABAAEAgAAA/wAAAAICRAEAOw==".decodeBase64()

        assertTrue(GifAnimationDetector.isAnimated(animated))
    }

    private fun String.decodeBase64(): ByteArray = java.util.Base64.getDecoder().decode(this)
}
