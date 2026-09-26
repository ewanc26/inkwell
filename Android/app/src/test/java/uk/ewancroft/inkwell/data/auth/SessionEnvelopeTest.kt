package uk.ewancroft.inkwell.data.auth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Envelope-level crypto, on a plain JVM AES key.
 *
 * These tests say nothing about the Android Keystore — see
 * `AndroidOAuthSessionStoreTest` for the store wired to a Keystore-shaped key
 * provider. What they do pin down is the format and the fail-closed behaviour:
 * that the envelope authenticates, that every way of corrupting it is rejected,
 * and that no plaintext survives in the encoded output.
 */
class SessionEnvelopeTest {

    private val plaintext =
        """{"accessToken":"secret-access-token","refreshToken":"secret-refresh-token"}"""
            .toByteArray()

    private fun aesKey(): SecretKey =
        KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun `seals and opens a round trip`() {
        val key = aesKey()

        val opened = SessionEnvelope.open(SessionEnvelope.seal(plaintext, key), key)

        assertArrayEquals(plaintext, opened)
    }

    @Test
    fun `encodes the schema version and a twelve byte IV in the header`() {
        val raw = Base64.getDecoder().decode(SessionEnvelope.seal(plaintext, aesKey()))

        assertEquals(SessionEnvelope.SCHEMA_VERSION, raw[0])
        assertEquals(12, raw[1].toInt())
        // header + IV + ciphertext + 16-byte GCM tag
        assertEquals(2 + 12 + plaintext.size + 16, raw.size)
    }

    @Test
    fun `never emits the plaintext and never repeats an IV`() {
        val key = aesKey()

        val first = Base64.getDecoder().decode(SessionEnvelope.seal(plaintext, key))
        val second = Base64.getDecoder().decode(SessionEnvelope.seal(plaintext, key))

        assertFalse(
            "sealed bytes must not contain the plaintext",
            String(first, Charsets.ISO_8859_1).contains("secret-access-token"),
        )
        // Same key, same plaintext, different nonce — so different ciphertext.
        assertNotEquals(
            first.copyOfRange(2, 14).toList(),
            second.copyOfRange(2, 14).toList(),
        )
        assertNotEquals(first.toList(), second.toList())
    }

    @Test
    fun `a flipped ciphertext byte fails closed`() {
        val key = aesKey()
        val raw = Base64.getDecoder().decode(SessionEnvelope.seal(plaintext, key))
        // Somewhere in the middle of the ciphertext, well clear of header and tag.
        raw[20] = (raw[20].toInt() xor 0x01).toByte()

        assertThrows(GeneralSecurityException::class.java) {
            SessionEnvelope.open(Base64.getEncoder().encodeToString(raw), key)
        }
    }

    @Test
    fun `a flipped GCM tag byte fails closed`() {
        val key = aesKey()
        val raw = Base64.getDecoder().decode(SessionEnvelope.seal(plaintext, key))
        raw[raw.lastIndex] = (raw[raw.lastIndex].toInt() xor 0x01).toByte()

        assertThrows(GeneralSecurityException::class.java) {
            SessionEnvelope.open(Base64.getEncoder().encodeToString(raw), key)
        }
    }

    @Test
    fun `a flipped IV byte fails closed`() {
        val key = aesKey()
        val raw = Base64.getDecoder().decode(SessionEnvelope.seal(plaintext, key))
        raw[5] = (raw[5].toInt() xor 0x01).toByte()

        assertThrows(GeneralSecurityException::class.java) {
            SessionEnvelope.open(Base64.getEncoder().encodeToString(raw), key)
        }
    }

    @Test
    fun `a rewritten schema version is rejected`() {
        val key = aesKey()
        val raw = Base64.getDecoder().decode(SessionEnvelope.seal(plaintext, key))
        raw[0] = 2

        // Rejected by the version check; the version is also bound in as AAD, so
        // an attacker who removed that check would still hit a tag failure.
        assertThrows(GeneralSecurityException::class.java) {
            SessionEnvelope.open(Base64.getEncoder().encodeToString(raw), key)
        }
    }

    @Test
    fun `a rewritten IV length is rejected`() {
        val key = aesKey()
        val raw = Base64.getDecoder().decode(SessionEnvelope.seal(plaintext, key))
        raw[1] = 16

        assertThrows(GeneralSecurityException::class.java) {
            SessionEnvelope.open(Base64.getEncoder().encodeToString(raw), key)
        }
    }

    @Test
    fun `another key cannot open the envelope`() {
        val envelope = SessionEnvelope.seal(plaintext, aesKey())

        assertThrows(GeneralSecurityException::class.java) {
            SessionEnvelope.open(envelope, aesKey())
        }
    }

    @Test
    fun `a truncated envelope is rejected before the cipher sees it`() {
        val key = aesKey()
        val raw = Base64.getDecoder().decode(SessionEnvelope.seal(plaintext, key))

        for (length in intArrayOf(0, 1, 2, 14, 2 + 12 + 15)) {
            assertThrows(GeneralSecurityException::class.java) {
                SessionEnvelope.open(
                    Base64.getEncoder().encodeToString(raw.copyOf(length)),
                    key,
                )
            }
        }
    }

    @Test
    fun `garbage that is not Base64 is rejected`() {
        assertThrows(GeneralSecurityException::class.java) {
            SessionEnvelope.open("not base64 !!!", aesKey())
        }
    }
}
