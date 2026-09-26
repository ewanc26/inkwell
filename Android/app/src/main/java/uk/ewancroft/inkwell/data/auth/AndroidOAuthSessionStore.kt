package uk.ewancroft.inkwell.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.kikin81.atproto.oauth.OAuthSession
import io.github.kikin81.atproto.oauth.OAuthSessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.GeneralSecurityException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stores the OAuth session — access and refresh tokens, DPoP key pair, nonces —
 * sealed under a non-exportable Android Keystore key.
 *
 * The session JSON is encrypted with AES-256-GCM into a [SessionEnvelope] and
 * that envelope is kept, Base64-encoded, in an ordinary app-private
 * [SharedPreferences] file. Plain preferences are safe here precisely because
 * nothing readable is in them: the file on its own — copied off the device,
 * pulled out of a backup, read by a rooted shell — decrypts to nothing without
 * the TEE- or StrongBox-bound key, which never leaves the Keystore.
 *
 * This replaces `EncryptedSharedPreferences` + `MasterKey`, both deprecated in
 * AndroidX Security Crypto. [LegacySessionMigration] carries an existing
 * sign-in across on first launch; see [load].
 *
 * ### Failure policy
 *
 * - [load] never throws. Anything it cannot decrypt it reports as "no session",
 *   which is the app's existing signed-out path, so key loss produces a login
 *   screen and not a crash loop. Ciphertext it can prove is unreadable it also
 *   deletes, so the store is clean for the next sign-in.
 * - [save] does throw. A session that silently fails to persist would present
 *   as a successful sign-in that evaporates on relaunch, over and over; the
 *   callers (`AtOAuth.completeLogin`, token refresh) already run inside
 *   `runCatching` and surface a sign-in error instead.
 * - [clear] never throws. Signing out locally has to succeed even when the
 *   Keystore does not answer.
 *
 * Nothing here logs a token, a key, a DID, or any envelope bytes.
 */
class AndroidOAuthSessionStore internal constructor(
    context: Context,
    private val keyProvider: SessionKeyProvider,
    private val legacy: LegacySessionMigration,
    private val ioDispatcher: CoroutineDispatcher,
) : OAuthSessionStore {

    constructor(appContext: Context) : this(
        context = appContext,
        keyProvider = AndroidKeystoreSessionKeyProvider(),
        legacy = LegacySessionMigration(appContext),
        ioDispatcher = Dispatchers.IO,
    )

    private val appContext: Context = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Migration is attempted at most once per process. It is idempotent and safe
     * to run on every launch, but when it fails it fails deterministically, and
     * repeating a deterministic failure on every [load] would only add noise.
     */
    private val legacyMigrationAttempted = AtomicBoolean(false)

    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the stored session, or `null` when there is none to return.
     *
     * On a device upgrading from the `EncryptedSharedPreferences` build this is
     * also where the one-time migration happens, because restoring the session
     * is the first thing the app does after launch.
     */
    override suspend fun load(): OAuthSession? = withContext(ioDispatcher) {
        val sealed = readSealedSession()
        if (sealed != null) {
            // A session in the new format supersedes anything still lying around
            // in the old one, e.g. a device that signed out and back in before
            // this build ever read the legacy file.
            discardLegacyIfPresent()
            return@withContext sealed
        }
        migrateLegacySession()
    }

    /** @throws GeneralSecurityException if the session cannot be sealed or persisted. */
    override suspend fun save(session: OAuthSession) {
        withContext(ioDispatcher) {
            val plaintext = json.encodeToString(OAuthSession.serializer(), session)
                .encodeToByteArray()
            val envelope = try {
                SessionEnvelope.seal(plaintext, keyProvider.sessionKey())
            } finally {
                plaintext.fill(0)
            }
            // commit(), not apply(): the caller is entitled to assume the session
            // survives an immediate process death, and a failed write must be
            // visible rather than queued into the void.
            if (!prefs.edit().putString(KEY_SEALED_SESSION, envelope).commit()) {
                throw GeneralSecurityException("could not persist the sealed OAuth session")
            }
            discardLegacyIfPresent()
        }
    }

    // commit() rather than apply() throughout: already off the main thread, and
    // a queued removal that a process death swallows would leave tokens on disk
    // after the user asked for them to be gone.
    @Suppress("ApplySharedPref")
    override suspend fun clear() {
        withContext(ioDispatcher) {
            prefs.edit().remove(KEY_SEALED_SESSION).commit()
            discardLegacyIfPresent()
        }
    }

    private fun readSealedSession(): OAuthSession? {
        val envelope = prefs.getString(KEY_SEALED_SESSION, null) ?: return null
        val key = try {
            keyProvider.sessionKey()
        } catch (_: GeneralSecurityException) {
            // The Keystore is unreachable, not the ciphertext wrong. The stored
            // envelope may well still be good, so report no session but leave it
            // where it is rather than destroying a recoverable sign-in.
            Log.w(TAG, "session key unavailable; treating the stored session as absent")
            return null
        }
        val plaintext = try {
            SessionEnvelope.open(envelope, key)
        } catch (_: GeneralSecurityException) {
            // A wrong key or a flipped byte. GCM does not fail transiently, so
            // this ciphertext will never open again: fail closed and drop it.
            Log.w(TAG, "stored session did not authenticate; discarding it")
            purgeSealedSession()
            return null
        }
        return decodeSession(plaintext) { purgeSealedSession() }
    }

    private fun migrateLegacySession(): OAuthSession? {
        if (!legacyMigrationAttempted.compareAndSet(false, true)) return null
        if (!legacy.hasLegacySession()) return null

        val legacyJson = legacy.readLegacySessionJson()
        if (legacyJson == null) {
            Log.w(TAG, "legacy session could not be read; discarding it")
            legacy.discardLegacySession()
            return null
        }
        val session = decodeSession(legacyJson.encodeToByteArray()) {
            Log.w(TAG, "legacy session JSON was not decodable; discarding it")
            legacy.discardLegacySession()
        } ?: return null

        val key = try {
            keyProvider.sessionKey()
        } catch (_: GeneralSecurityException) {
            // No key means nothing to re-seal with. Leave the legacy file intact
            // so a later launch on a healthy Keystore can still migrate it.
            Log.w(TAG, "session key unavailable; deferring the legacy migration")
            return null
        }

        val legacyBytes = legacyJson.encodeToByteArray()
        val envelope = try {
            SessionEnvelope.seal(legacyBytes, key)
        } catch (_: GeneralSecurityException) {
            Log.w(TAG, "could not seal the legacy session; deferring the migration")
            return null
        } finally {
            legacyBytes.fill(0)
        }
        if (!prefs.edit().putString(KEY_SEALED_SESSION, envelope).commit()) {
            Log.w(TAG, "could not persist the migrated session; deferring the migration")
            return null
        }

        // Read the committed envelope back and open it before the legacy file is
        // destroyed. This is the whole point of ordering the migration this way:
        // the old copy is only ever deleted once a verified new copy exists.
        val verified = runCatching {
            val stored = prefs.getString(KEY_SEALED_SESSION, null)
                ?: return@runCatching null
            SessionEnvelope.open(stored, key).decodeToString()
        }.getOrNull()
        if (verified != legacyJson) {
            Log.w(TAG, "migrated session failed verification; keeping the legacy copy")
            purgeSealedSession()
            return null
        }

        legacy.discardLegacySession()
        Log.i(TAG, "migrated the stored OAuth session to the Keystore envelope")
        return session
    }

    /**
     * Parses session JSON out of [plaintext], zeroing the buffer either way, and
     * runs [onUndecodable] if the bytes are not a session at all.
     */
    private fun decodeSession(
        plaintext: ByteArray,
        onUndecodable: () -> Unit,
    ): OAuthSession? = try {
        json.decodeFromString(OAuthSession.serializer(), plaintext.decodeToString())
    } catch (_: IllegalArgumentException) {
        // Covers kotlinx SerializationException, which extends this.
        onUndecodable()
        null
    } finally {
        plaintext.fill(0)
    }

    @Suppress("ApplySharedPref")
    private fun purgeSealedSession() {
        prefs.edit().remove(KEY_SEALED_SESSION).commit()
    }

    private fun discardLegacyIfPresent() {
        if (legacy.hasLegacySession()) legacy.discardLegacySession()
    }

    internal companion object {
        /**
         * Ordinary app-private preferences holding only the sealed envelope.
         * Excluded from cloud backup and device transfer in
         * `res/xml/backup_rules.xml` and `res/xml/backup_rules_legacy.xml`:
         * restoring ciphertext whose Keystore key cannot travel with it would
         * only produce an unopenable blob and a confusing signed-out state.
         */
        internal const val PREFS_NAME = "inkwell_oauth_session_sealed"

        internal const val KEY_SEALED_SESSION = "sealed_session_v1"

        private const val TAG = "SessionStore"
    }
}
