package uk.ewancroft.inkwell.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.kikin81.atproto.oauth.AtOAuth
import io.github.kikin81.atproto.oauth.OAuthSessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import uk.ewancroft.inkwell.data.auth.AndroidOAuthSessionStore
import uk.ewancroft.inkwell.shared.oauth.InkwellOAuthScopes
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OAuthModule {

    private const val CLIENT_METADATA_URL =
        "https://inkwell.ewancroft.uk/client-metadata.json"
    private const val REDIRECT_URI = "uk.ewancroft.inkwell:/callback"

    /**
     * The OAuth scope string sent at authorization time.
     *
     * Composed in shared KMP so Android, iOS and the three hosted `client-metadata.json`
     * copies cannot drift. Whether the Standard.site block is requested as four granular
     * `repo:` scopes or as `include:site.standard.authFull` is decided by
     * `StandardSitePermissionSets.USE_PERMISSION_SET`; both forms are declared in client
     * metadata, so flipping it needs no metadata redeploy.
     */
    internal val SCOPE = InkwellOAuthScopes.runtimeScopeString()

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(CIO)

    @Provides
    @Singleton
    fun provideSessionStore(
        @ApplicationContext context: Context
    ): OAuthSessionStore = AndroidOAuthSessionStore(context)

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
        scope = SCOPE,
    )
}
