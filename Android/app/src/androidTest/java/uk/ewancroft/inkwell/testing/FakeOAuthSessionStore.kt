package uk.ewancroft.inkwell.testing

import io.github.kikin81.atproto.oauth.OAuthSession
import io.github.kikin81.atproto.oauth.OAuthSessionStore
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory stand-in for `AndroidOAuthSessionStore`.
 *
 * Two reasons instrumentation tests must not use the real store:
 *
 * 1. It seals sessions under a non-exportable Android Keystore key. A test
 *    that wrote through it would be asserting on device key material, not on
 *    app behaviour, and would leave a sealed envelope behind in app-private
 *    SharedPreferences between runs.
 * 2. Every test in this suite starts *signed out*, which for the real store
 *    means "whatever the last run happened to leave on disk". An empty
 *    in-memory store makes the unauthenticated starting point a fact rather
 *    than an assumption.
 *
 * Returning null from [load] is also what keeps the suite hermetic: both
 * `PdsRepository.validateRestoredSession()` and every feed loader bail out
 * before their first network call when there is no session, so no test needs
 * real AT Protocol credentials and none can mutate a real repository.
 */
@Singleton
class FakeOAuthSessionStore @Inject constructor() : OAuthSessionStore {

    private val stored = AtomicReference<OAuthSession?>(null)

    /** Number of times the app asked for a session — a cheap delivery probe. */
    @Volatile
    var loadCount: Int = 0
        private set

    override suspend fun load(): OAuthSession? {
        loadCount++
        return stored.get()
    }

    override suspend fun save(session: OAuthSession) {
        stored.set(session)
    }

    override suspend fun clear() {
        stored.set(null)
    }

    /** Resets the fake between tests so state cannot leak across them. */
    fun reset() {
        stored.set(null)
        loadCount = 0
    }
}
