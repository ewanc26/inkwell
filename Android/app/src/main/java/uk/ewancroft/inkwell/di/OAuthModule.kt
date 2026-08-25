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
import uk.ewancroft.inkwell.shared.oauth.OAuthScopes
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OAuthModule {

    private const val CLIENT_METADATA_URL =
        "https://inkwell.ewancroft.uk/client-metadata.json"
    private const val REDIRECT_URI = "uk.ewancroft.inkwell:/callback"

    private const val SCOPE =
        "atproto blob:*/* repo:site.standard.publication repo:site.standard.document " +
            "repo:site.standard.graph.subscription repo:site.standard.graph.recommend " +
            "repo:app.userinput.discussion " +
            OAuthScopes.REPO_USER +
            "repo:app.bsky.graph.block?action=create&action=delete " +
            "rpc:app.bsky.graph.muteActor?aud=did:web:api.bsky.app%23bsky_appview " +
            "rpc:app.bsky.graph.unmuteActor?aud=did:web:api.bsky.app%23bsky_appview " +
            "rpc:app.bsky.graph.getMutes?aud=did:web:api.bsky.app%23bsky_appview " +
            "rpc:app.bsky.graph.getBlocks?aud=did:web:api.bsky.app%23bsky_appview"

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
