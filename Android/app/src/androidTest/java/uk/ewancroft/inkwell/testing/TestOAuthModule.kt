package uk.ewancroft.inkwell.testing

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.kikin81.atproto.oauth.AtOAuth
import io.github.kikin81.atproto.oauth.OAuthSessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import uk.ewancroft.inkwell.di.OAuthModule
import uk.ewancroft.inkwell.shared.oauth.InkwellOAuthScopes
import javax.inject.Singleton

/**
 * Replaces [OAuthModule] for the whole instrumentation suite.
 *
 * `AtOAuth` is final, so it cannot be faked — and it should not be: keeping the
 * real object means the tests exercise the real "is this callback mine, and has
 * it already been spent?" routing in `MainActivity` rather than a stub of it.
 * What is replaced is everything around it:
 *
 * - the session store, so nothing touches the Android Keystore and every test
 *   starts genuinely signed out (see [FakeOAuthSessionStore]);
 * - the ktor transport, so token exchange and PDS discovery can never leave the
 *   device. A callback delivered by a test therefore always fails the exchange,
 *   which is the point: the assertion is that the callback was *routed* exactly
 *   once, not that a fabricated login succeeded.
 *
 * The client id and redirect URI are deliberately the production values — they
 * are part of the callback contract under test, not incidental configuration.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [OAuthModule::class])
object TestOAuthModule {

    private const val CLIENT_METADATA_URL = "https://inkwell.ewancroft.uk/client-metadata.json"
    private const val REDIRECT_URI = "uk.ewancroft.inkwell:/callback"

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(
        MockEngine {
            respondError(
                HttpStatusCode.ServiceUnavailable,
                "Instrumentation tests never reach a real PDS.",
            )
        },
    )

    @Provides
    @Singleton
    fun provideSessionStore(fake: FakeOAuthSessionStore): OAuthSessionStore = fake

    @Provides
    @Singleton
    fun provideAtOAuth(
        sessionStore: OAuthSessionStore,
        httpClient: HttpClient,
    ): AtOAuth = AtOAuth(
        clientMetadataUrl = CLIENT_METADATA_URL,
        redirectUri = REDIRECT_URI,
        sessionStore = sessionStore,
        httpClient = httpClient,
        scope = InkwellOAuthScopes.runtimeScopeString(),
    )
}
