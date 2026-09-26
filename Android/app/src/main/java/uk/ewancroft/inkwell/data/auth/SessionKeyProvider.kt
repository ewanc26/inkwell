package uk.ewancroft.inkwell.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Supplies the AES-256 key that seals the stored OAuth session.
 *
 * Exists as an interface purely so JVM unit tests can supply an in-process key:
 * Robolectric has no `AndroidKeyStore` provider, and production must never have
 * a code path that accepts key material from outside the Keystore.
 */
internal interface SessionKeyProvider {
    /**
     * Returns the session key, generating it on first use.
     *
     * @throws java.security.GeneralSecurityException if the key cannot be
     * fetched or created. Callers treat that as "storage unavailable", never as
     * "no session".
     */
    fun sessionKey(): SecretKey
}

/**
 * Non-exportable AES-256-GCM key held in the Android Keystore.
 *
 * The key is generated inside the Keystore and is never seen by this process —
 * only opaque handles to it are. That is what makes it safe to keep the sealed
 * session in ordinary app-private [android.content.SharedPreferences]: the file
 * on its own (copied off the device, pulled out of a backup, read by a rooted
 * shell) decrypts to nothing without the hardware- or TEE-bound key.
 */
internal class AndroidKeystoreSessionKeyProvider(
    private val alias: String = DEFAULT_ALIAS,
) : SessionKeyProvider {

    @Synchronized
    override fun sessionKey(): SecretKey {
        val keystore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = try {
            if (keystore.containsAlias(alias)) keystore.getKey(alias, null) as? SecretKey else null
        } catch (_: UnrecoverableKeyException) {
            // The key material is gone even though the alias survives — a restored
            // backup, a cleared secure lock screen. Anything sealed with it is
            // already permanently unreadable, so replace it and let the read path
            // fail closed rather than leaving the store wedged.
            runCatching { keystore.deleteEntry(alias) }
            null
        }
        return existing ?: generate()
    }

    private fun generate(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                // Keystore, not this process, draws a fresh random IV for every
                // encryption and rejects a caller-supplied one. That is what makes
                // GCM nonce reuse — which would be catastrophic for confidentiality
                // and authenticity alike — structurally impossible here.
                .setRandomizedEncryptionRequired(true)
                // No biometric/lock-screen gate: background token refresh and
                // notification polling both need the session while the device is
                // locked, exactly like the iOS Keychain's AfterFirstUnlock class.
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        const val DEFAULT_ALIAS = "inkwell_oauth_session_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_SIZE_BITS = 256
    }
}
