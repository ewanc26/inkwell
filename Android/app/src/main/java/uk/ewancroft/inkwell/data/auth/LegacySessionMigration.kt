package uk.ewancroft.inkwell.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Read-and-destroy access to the pre-Keystore-envelope session file.
 *
 * Inkwell used to keep the OAuth session JSON in an `EncryptedSharedPreferences`
 * file named [LEGACY_PREFS_NAME], sealed by a `MasterKey`. Both APIs are
 * deprecated in AndroidX Security Crypto, so [AndroidOAuthSessionStore] now
 * seals the same JSON itself with a [SessionEnvelope] under a key it owns in the
 * Android Keystore.
 *
 * This class is the *only* remaining use of the deprecated APIs, and it is
 * write-never: it exists to hand an already-signed-in user's session across to
 * the new format once, then delete the old file. When no device still has a
 * legacy file, this class and the `security-crypto` dependency can both go.
 */
internal class LegacySessionMigration(appContext: Context) {

    private val appContext: Context = appContext.applicationContext

    /**
     * Whether a legacy file is still on disk.
     *
     * Checks the file rather than opening the preferences: opening would build a
     * Tink keyset and a Keystore `MasterKey` on devices that never had a legacy
     * session at all, which is both wasteful and a way to fail for no reason.
     */
    fun hasLegacySession(): Boolean = legacyFile().exists()

    /**
     * Decrypts the legacy session JSON, or returns `null` if it cannot be read.
     *
     * `null` covers every unrecoverable case at once — no entry, a corrupt Tink
     * keyset, a `MasterKey` that a backup restore or a cleared lock screen took
     * away — because the caller's response to all of them is identical: discard
     * the file and let the user sign in again.
     */
    @Suppress("DEPRECATION")
    fun readLegacySessionJson(): String? = runCatching {
        legacyPreferences().getString(LEGACY_SESSION_KEY, null)
    }.getOrNull()

    /**
     * Removes the legacy file, its backup copy, and its in-memory cache.
     *
     * Safe to call when there is nothing to remove, and safe to call twice. Best
     * effort by design: this runs after the session already exists in the new
     * format, so a failure here must never take down a working sign-in. It does
     * try to blank the entry through the deprecated API first, so that even if
     * the file itself somehow survives, the tokens inside it do not.
     */
    @Suppress("DEPRECATION")
    fun discardLegacySession() {
        runCatching { legacyPreferences().edit().clear().commit() }
        runCatching { appContext.deleteSharedPreferences(LEGACY_PREFS_NAME) }
        runCatching { legacyFile().delete() }
        runCatching { File(legacyFile().parentFile, "$LEGACY_PREFS_NAME.xml.bak").delete() }
    }

    @Suppress("DEPRECATION")
    private fun legacyPreferences() = EncryptedSharedPreferences.create(
        appContext,
        LEGACY_PREFS_NAME,
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private fun legacyFile(): File =
        File(File(appContext.dataDir, "shared_prefs"), "$LEGACY_PREFS_NAME.xml")

    companion object {
        /**
         * The legacy `EncryptedSharedPreferences` file name. Still named in
         * `res/xml/backup_rules.xml` and `res/xml/backup_rules_legacy.xml` so a
         * device that has not launched the new build yet keeps its exclusion.
         */
        const val LEGACY_PREFS_NAME = "inkwell_oauth_session"

        /** The single entry the legacy file ever held. */
        const val LEGACY_SESSION_KEY = "oauth_session_json"
    }
}
