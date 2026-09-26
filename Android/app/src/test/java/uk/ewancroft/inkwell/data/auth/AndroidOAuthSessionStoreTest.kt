package uk.ewancroft.inkwell.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.kikin81.atproto.oauth.OAuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.Base64

/**
 * The session store end to end: envelope round-trip, one-time migration off the
 * deprecated `EncryptedSharedPreferences` file, and fail-closed behaviour when
 * the key or the ciphertext is gone.
 *
 * [FakeAndroidKeyStore] stands in for the real Keystore, which Robolectric does
 * not provide. It reproduces the API the production key provider uses — so the
 * real [AndroidKeystoreSessionKeyProvider], the real [SessionEnvelope], and the
 * real `EncryptedSharedPreferences` legacy reader all run here — but not the
 * Keystore's security properties. Nothing in this file is evidence about
 * hardware key isolation; that needs a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidOAuthSessionStoreTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val json = Json { ignoreUnknownKeys = true }

    private val session = OAuthSession(
        accessToken = "access-token-aaaa",
        refreshToken = "refresh-token-bbbb",
        did = "did:plc:alice",
        handle = "alice.example",
        pdsUrl = "https://pds.example",
        tokenEndpoint = "https://auth.example/token",
        revocationEndpoint = "https://auth.example/revoke",
        clientId = "https://inkwell.ewancroft.uk/client-metadata.json",
        dpopPrivateKey = byteArrayOf(1, 2, 3, 4, 5),
        dpopPublicKey = byteArrayOf(6, 7, 8, 9, 10),
        authServerNonce = "auth-nonce",
        clockOffsetSeconds = 7L,
        pdsNonce = "pds-nonce",
    )

    @Before
    fun setUp() {
        FakeAndroidKeyStore.install()
        FakeAndroidKeyStore.reset()
        context.deleteSharedPreferences(AndroidOAuthSessionStore.PREFS_NAME)
        context.deleteSharedPreferences(LegacySessionMigration.LEGACY_PREFS_NAME)
        sharedPrefsFile(AndroidOAuthSessionStore.PREFS_NAME).delete()
        legacyFile().delete()
    }

    // ── New format ──────────────────────────────────────────────────────

    @Test
    fun `saves and loads a session through the Keystore envelope`() = runTest {
        val store = newStore()

        store.save(session)

        assertSameSession(session, newStore().load())
    }

    @Test
    fun `persists only a versioned envelope and never the tokens`() = runTest {
        newStore().save(session)

        val stored = sealedEnvelope()
        assertNotNull("a sealed envelope must be written", stored)
        val raw = Base64.getDecoder().decode(stored)
        assertEquals(SessionEnvelope.SCHEMA_VERSION, raw[0])
        assertEquals(12, raw[1].toInt())

        val onDisk = sharedPrefsFile(AndroidOAuthSessionStore.PREFS_NAME).readText()
        for (secret in listOf("access-token-aaaa", "refresh-token-bbbb", "did:plc:alice")) {
            assertFalse(
                "$secret must not appear in the preferences file",
                onDisk.contains(secret),
            )
        }
    }

    @Test
    fun `a fresh install has no session and does not create one`() = runTest {
        assertNull(newStore().load())
        assertNull(sealedEnvelope())
    }

    @Test
    fun `a refreshed session replaces the stored one`() = runTest {
        val store = newStore()
        store.save(session)
        val first = sealedEnvelope()

        // The token-refresh path: same store, new tokens and nonces.
        val refreshed = session.copy(
            accessToken = "access-token-cccc",
            refreshToken = "refresh-token-dddd",
            pdsNonce = "pds-nonce-2",
        )
        store.save(refreshed)

        assertNotEquals("the envelope must be rewritten", first, sealedEnvelope())
        assertSameSession(refreshed, newStore().load())
        // And the superseded tokens are not left lying in the file.
        val onDisk = sharedPrefsFile(AndroidOAuthSessionStore.PREFS_NAME).readText()
        assertFalse(onDisk.contains("access-token-aaaa"))
    }

    @Test
    fun `clear removes the stored session`() = runTest {
        val store = newStore()
        store.save(session)

        store.clear()

        assertNull(sealedEnvelope())
        assertNull(newStore().load())
    }

    @Test
    fun `a tampered envelope fails closed and is purged`() = runTest {
        newStore().save(session)
        val raw = Base64.getDecoder().decode(sealedEnvelope())
        raw[20] = (raw[20].toInt() xor 0x01).toByte()
        writeSealedEnvelope(Base64.getEncoder().encodeToString(raw))

        assertNull(newStore().load())
        assertNull("unopenable ciphertext must not be left behind", sealedEnvelope())
    }

    @Test
    fun `a lost Keystore key fails closed and a fresh sign in still saves`() = runTest {
        newStore().save(session)

        // Device restore, reinstall, or a cleared secure lock screen.
        FakeAndroidKeyStore.deleteKey(AndroidKeystoreSessionKeyProvider.DEFAULT_ALIAS)

        val store = newStore()
        assertNull(store.load())
        assertNull(sealedEnvelope())

        store.save(session)
        assertSameSession(session, newStore().load())
    }

    @Test
    fun `undecodable stored JSON is discarded`() = runTest {
        writeSealedEnvelope(
            SessionEnvelope.seal(
                "not a session".toByteArray(),
                AndroidKeystoreSessionKeyProvider().sessionKey(),
            ),
        )

        assertNull(newStore().load())
        assertNull(sealedEnvelope())
    }

    // ── Migration off EncryptedSharedPreferences ─────────────────────────

    @Test
    fun `migrates a legacy session, verifies it, then deletes the legacy file`() = runTest {
        writeLegacySession(json.encodeToString(OAuthSession.serializer(), session))
        assertTrue(legacyFile().exists())

        val migrated = newStore().load()

        assertSameSession(session, migrated)
        assertNotNull("the migrated session must be sealed into the new file", sealedEnvelope())
        assertFalse("the legacy file must be gone", legacyFile().exists())
        // And it is genuinely readable from the new format, not just returned
        // from the in-flight migration.
        assertSameSession(session, newStore().load())
    }

    @Test
    fun `migration is idempotent across relaunches`() = runTest {
        writeLegacySession(json.encodeToString(OAuthSession.serializer(), session))
        assertSameSession(session, newStore().load())
        val afterFirst = sealedEnvelope()

        // Every later "launch" builds a fresh store and finds nothing to migrate.
        repeat(3) { assertSameSession(session, newStore().load()) }

        assertEquals(afterFirst, sealedEnvelope())
        assertFalse(legacyFile().exists())
    }

    @Test
    fun `a legacy file left beside an existing sealed session is discarded`() = runTest {
        newStore().save(session)
        writeLegacySession(json.encodeToString(OAuthSession.serializer(), session))
        assertTrue(legacyFile().exists())

        assertSameSession(session, newStore().load())

        assertFalse("a superseded legacy file must not survive", legacyFile().exists())
    }

    @Test
    fun `a legacy session whose MasterKey is gone is discarded without throwing`() = runTest {
        writeLegacySession(json.encodeToString(OAuthSession.serializer(), session))
        FakeAndroidKeyStore.deleteKey(ANDROIDX_MASTER_KEY_ALIAS)

        assertNull(newStore().load())
        assertFalse(legacyFile().exists())
        assertNull(sealedEnvelope())
    }

    @Test
    fun `corrupt legacy JSON is discarded rather than migrated`() = runTest {
        writeLegacySession("{ this is not a session }")

        assertNull(newStore().load())
        assertFalse(legacyFile().exists())
        assertNull(sealedEnvelope())
    }

    @Test
    fun `clear removes a legacy file that was never migrated`() = runTest {
        writeLegacySession(json.encodeToString(OAuthSession.serializer(), session))

        newStore().clear()

        assertFalse(legacyFile().exists())
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /** A store as the app builds it, but on the test dispatcher. */
    private fun newStore() = AndroidOAuthSessionStore(
        context = context,
        keyProvider = AndroidKeystoreSessionKeyProvider(),
        legacy = LegacySessionMigration(context),
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun sealedPrefs() = context.getSharedPreferences(
        AndroidOAuthSessionStore.PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private fun sealedEnvelope(): String? =
        sealedPrefs().getString(AndroidOAuthSessionStore.KEY_SEALED_SESSION, null)

    private fun writeSealedEnvelope(envelope: String) {
        assertTrue(
            sealedPrefs().edit()
                .putString(AndroidOAuthSessionStore.KEY_SEALED_SESSION, envelope)
                .commit(),
        )
    }

    @Suppress("DEPRECATION")
    private fun writeLegacySession(sessionJson: String) {
        val prefs = EncryptedSharedPreferences.create(
            context,
            LegacySessionMigration.LEGACY_PREFS_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        assertTrue(
            prefs.edit()
                .putString(LegacySessionMigration.LEGACY_SESSION_KEY, sessionJson)
                .commit(),
        )
        assertTrue(legacyFile().exists())
    }

    private fun legacyFile() = sharedPrefsFile(LegacySessionMigration.LEGACY_PREFS_NAME)

    private fun sharedPrefsFile(name: String) =
        File(File(context.dataDir, "shared_prefs"), "$name.xml")

    private fun assertSameSession(expected: OAuthSession, actual: OAuthSession?) {
        assertNotNull("expected a session", actual)
        requireNotNull(actual)
        assertEquals(expected.accessToken, actual.accessToken)
        assertEquals(expected.refreshToken, actual.refreshToken)
        assertEquals(expected.did, actual.did)
        assertEquals(expected.handle, actual.handle)
        assertEquals(expected.pdsUrl, actual.pdsUrl)
        assertEquals(expected.tokenEndpoint, actual.tokenEndpoint)
        assertEquals(expected.revocationEndpoint, actual.revocationEndpoint)
        assertEquals(expected.clientId, actual.clientId)
        assertArrayEquals(expected.dpopPrivateKey, actual.dpopPrivateKey)
        assertArrayEquals(expected.dpopPublicKey, actual.dpopPublicKey)
        assertEquals(expected.authServerNonce, actual.authServerNonce)
        assertEquals(expected.clockOffsetSeconds, actual.clockOffsetSeconds)
        assertEquals(expected.pdsNonce, actual.pdsNonce)
    }

    private companion object {
        /** `MasterKey.DEFAULT_MASTER_KEY_ALIAS`, named here so the test does not
         *  have to touch the deprecated constant. */
        const val ANDROIDX_MASTER_KEY_ALIAS = "_androidx_security_master_key_"
    }
}
