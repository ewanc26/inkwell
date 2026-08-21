package uk.ewancroft.inkwell.util

import android.util.Base64
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Verifies a customisation unlock key against an embedded public key.
 * There's no server: someone pays the one-off fee via Ko-fi/GitHub
 * Sponsors (see tools/license/README.md), and a key is issued by hand
 * with tools/license/generate-key.mjs, which holds the matching private
 * key -- never committed to this repo.
 *
 * This is an honour-system unlock, not DRM: Inkwell is AGPL-3.0, so
 * anyone willing to build from source can bypass this check entirely,
 * same as removing any other code. What this genuinely prevents is
 * someone guessing or mass-generating "free" keys -- an EC signature
 * can't be forged without the private key, unlike a plain string check.
 *
 * Mirrors iOS LicenseVerifier.swift.
 */
object LicenseVerifier {
    /** Must exactly match LICENSE_MESSAGE in tools/license/generate-key.mjs
     *  and iOS's LicenseVerifier.message. */
    const val MESSAGE = "inkwell-customisation-unlock-v1"

    /** The public half of the signing key in tools/license/README.md.
     *  X.509 SubjectPublicKeyInfo DER, base64-encoded. */
    private const val PUBLIC_KEY_BASE64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEnyaGdl6jsXfDiJBQVTVdtHmEAc93wXIVtXviMOEKBjiW254v5jDNg2L7h1Fu/kjZwnKkcvlzbQuuBOqOC9O0TA=="

    /** @param licenseKey base64url-encoded DER ECDSA-P256-SHA256 signature
     *  over [MESSAGE], as produced by generate-key.mjs. */
    fun isValid(licenseKey: String): Boolean = try {
        val keyBytes = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT)
        val publicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(keyBytes))

        val base64 = licenseKey.trim().replace('-', '+').replace('_', '/')
        val padded = base64 + "=".repeat((4 - base64.length % 4) % 4)
        val signatureBytes = Base64.decode(padded, Base64.DEFAULT)

        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(MESSAGE.toByteArray(Charsets.UTF_8))
        verifier.verify(signatureBytes)
    } catch (e: Exception) {
        false
    }
}
