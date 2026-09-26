package uk.ewancroft.inkwell.data.auth

import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The versioned AES-GCM envelope that the stored OAuth session lives inside.
 *
 * Layout, before Base64:
 *
 * ```
 * byte 0        schema version
 * byte 1        IV length in bytes (always 12)
 * bytes 2..13   GCM IV / nonce
 * bytes 14..n   ciphertext with the 16-byte GCM tag appended
 * ```
 *
 * The plaintext is the same session JSON the previous `EncryptedSharedPreferences`
 * implementation stored, so migrating a session is a re-seal, not a re-encode.
 *
 * Two deliberate properties:
 *
 * - **The schema version is authenticated.** It sits outside the ciphertext so
 *   it can be read before a key is needed, but it is also fed to the cipher as
 *   AAD, so flipping it — or replaying a future v2 blob as a v1 one — makes the
 *   tag check fail instead of silently changing how the bytes are interpreted.
 * - **Every failure is a failure.** A wrong key, a truncated file, a flipped
 *   byte, an unknown version: all throw [GeneralSecurityException]. There is no
 *   path that returns partial or unauthenticated plaintext, and no exception
 *   message carries key material, ciphertext, or plaintext.
 */
internal object SessionEnvelope {

    /** Current envelope schema. Bump only alongside a read path for the old one. */
    const val SCHEMA_VERSION: Byte = 1

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8
    private const val IV_LENGTH_BYTES = 12
    private const val HEADER_LENGTH_BYTES = 2

    /**
     * Seals [plaintext] under [key] and returns the Base64 envelope to persist.
     *
     * The IV is never chosen here. Keys from
     * [AndroidKeystoreSessionKeyProvider] are generated with
     * `setRandomizedEncryptionRequired(true)`, which makes the Keystore draw a
     * fresh IV per operation and *reject* a caller-supplied one — so GCM nonce
     * reuse, which would be catastrophic for both confidentiality and
     * authenticity, is structurally impossible rather than merely avoided.
     *
     * @throws GeneralSecurityException if the key or provider cannot encrypt.
     */
    fun seal(plaintext: ByteArray, key: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv ?: throw GeneralSecurityException("cipher produced no IV")
        if (iv.size != IV_LENGTH_BYTES) {
            throw GeneralSecurityException("unexpected GCM IV length ${iv.size}")
        }
        cipher.updateAAD(byteArrayOf(SCHEMA_VERSION))
        val ciphertext = cipher.doFinal(plaintext)

        val envelope = ByteArray(HEADER_LENGTH_BYTES + iv.size + ciphertext.size)
        envelope[0] = SCHEMA_VERSION
        envelope[1] = iv.size.toByte()
        iv.copyInto(envelope, HEADER_LENGTH_BYTES)
        ciphertext.copyInto(envelope, HEADER_LENGTH_BYTES + iv.size)
        return Base64.getEncoder().encodeToString(envelope)
    }

    /**
     * Opens an envelope produced by [seal].
     *
     * @throws GeneralSecurityException if [encoded] is not a well-formed
     * envelope of a supported version, or if the tag does not verify under
     * [key]. Callers must treat that as "this session is gone", never as
     * "retry" — GCM authentication does not fail transiently.
     */
    fun open(encoded: String, key: SecretKey): ByteArray {
        val envelope = try {
            Base64.getDecoder().decode(encoded)
        } catch (_: IllegalArgumentException) {
            throw GeneralSecurityException("stored session is not valid Base64")
        }
        if (envelope.size < HEADER_LENGTH_BYTES) {
            throw GeneralSecurityException("stored session has no envelope header")
        }
        val version = envelope[0]
        if (version != SCHEMA_VERSION) {
            throw GeneralSecurityException("unsupported session schema version $version")
        }
        val ivLength = envelope[1].toInt()
        if (ivLength != IV_LENGTH_BYTES) {
            throw GeneralSecurityException("unexpected GCM IV length $ivLength")
        }
        // An envelope shorter than header + IV + tag cannot authenticate, so
        // reject it before handing anything to the cipher.
        if (envelope.size < HEADER_LENGTH_BYTES + ivLength + TAG_LENGTH_BYTES) {
            throw GeneralSecurityException("stored session is truncated")
        }

        val iv = envelope.copyOfRange(HEADER_LENGTH_BYTES, HEADER_LENGTH_BYTES + ivLength)
        val ciphertext = envelope.copyOfRange(HEADER_LENGTH_BYTES + ivLength, envelope.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        cipher.updateAAD(byteArrayOf(version))
        return cipher.doFinal(ciphertext)
    }
}
