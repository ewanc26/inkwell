package uk.ewancroft.inkwell.data.auth

import android.security.keystore.KeyGenParameterSpec
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Robolectric has no `AndroidKeyStore` provider, so production code that talks
 * to the Keystore cannot run under JVM unit tests as-is. This registers a
 * stand-in provider of the same name backed by an in-memory map of raw keys.
 *
 * It reproduces the *API surface* the store depends on — `KeyStore` lookup by
 * alias and `KeyGenerator.getInstance("AES", "AndroidKeyStore")` driven by a
 * [KeyGenParameterSpec] — so the real [AndroidKeystoreSessionKeyProvider] and
 * the real GCM envelope code are exercised. It deliberately does NOT reproduce
 * the Keystore's security properties: keys here are ordinary in-process
 * [SecretKeySpec]s, are exportable, and `setRandomizedEncryptionRequired` is
 * not enforced. Tests must not treat a pass here as evidence about hardware
 * key isolation.
 */
object FakeAndroidKeyStore {

    private val keys = Collections.synchronizedMap(linkedMapOf<String, Key>())

    fun install() {
        if (Security.getProvider(NAME) == null) {
            Security.addProvider(FakeProvider())
        }
    }

    fun reset() {
        keys.clear()
    }

    /** Simulates a Keystore key being lost (device restore, biometric reset, reinstall). */
    fun deleteKey(alias: String) {
        keys.remove(alias)
    }

    fun containsKey(alias: String): Boolean = keys.containsKey(alias)

    internal fun put(alias: String, key: Key) {
        keys[alias] = key
    }

    internal fun get(alias: String): Key? = keys[alias]

    internal fun remove(alias: String) {
        keys.remove(alias)
    }

    internal fun aliases(): List<String> = synchronized(keys) { keys.keys.toList() }

    internal fun size(): Int = keys.size

    private const val NAME = "AndroidKeyStore"

    class FakeProvider : Provider(NAME, 1.0, "Robolectric stand-in for AndroidKeyStore") {
        init {
            put("KeyStore.$NAME", Spi::class.java.name)
            put("KeyGenerator.AES", Generator::class.java.name)
        }
    }

    class Spi : KeyStoreSpi() {
        override fun engineGetKey(alias: String, password: CharArray?): Key? = get(alias)

        override fun engineGetCertificateChain(alias: String): Array<Certificate>? = null

        override fun engineGetCertificate(alias: String): Certificate? = null

        override fun engineGetCreationDate(alias: String): Date? =
            if (containsKey(alias)) Date(0) else null

        override fun engineSetKeyEntry(
            alias: String,
            key: Key,
            password: CharArray?,
            chain: Array<out Certificate>?,
        ) = put(alias, key)

        override fun engineSetKeyEntry(alias: String, key: ByteArray, chain: Array<out Certificate>?) =
            put(alias, SecretKeySpec(key, "AES"))

        override fun engineSetCertificateEntry(alias: String, cert: Certificate?) =
            throw UnsupportedOperationException()

        override fun engineDeleteEntry(alias: String) = remove(alias)

        override fun engineAliases(): Enumeration<String> =
            Collections.enumeration(aliases())

        override fun engineContainsAlias(alias: String): Boolean = containsKey(alias)

        override fun engineSize(): Int = size()

        override fun engineIsKeyEntry(alias: String): Boolean = containsKey(alias)

        override fun engineIsCertificateEntry(alias: String): Boolean = false

        override fun engineGetCertificateAlias(cert: Certificate?): String? = null

        override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit

        override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit
    }

    class Generator : KeyGeneratorSpi() {
        private var alias: String? = null
        private var keySizeBits: Int = 256

        override fun engineInit(random: SecureRandom?) = Unit

        override fun engineInit(params: AlgorithmParameterSpec?, random: SecureRandom?) {
            val spec = params as? KeyGenParameterSpec
                ?: throw IllegalArgumentException("expected KeyGenParameterSpec")
            alias = spec.keystoreAlias
            if (spec.keySize > 0) keySizeBits = spec.keySize
        }

        override fun engineInit(keysize: Int, random: SecureRandom?) {
            keySizeBits = keysize
        }

        override fun engineGenerateKey(): SecretKey {
            val raw = ByteArray(keySizeBits / 8)
            SecureRandom().nextBytes(raw)
            val key = SecretKeySpec(raw, "AES")
            alias?.let { put(it, key) }
            return key
        }
    }
}
