package uk.ewancroft.inkwell.testing

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import uk.ewancroft.inkwell.data.remote.ConstellationClient
import uk.ewancroft.inkwell.di.NetworkModule
import javax.inject.Singleton

/**
 * Replaces [NetworkModule] so the injected OkHttp client is a wall, not a
 * transport.
 *
 * Every request is short-circuited to `503` without opening a socket. Screens
 * therefore render their real offline/error states — which is what "launches
 * without crashing" should actually mean — while the suite stays deterministic
 * and stays off the network regardless of what the emulator can reach.
 *
 * `ConstellationClient` is re-provided unchanged because `@TestInstallIn`
 * replaces a module wholesale, not one binding. It holds its own internal
 * client and so is not covered by the wall; nothing in this suite navigates to
 * a screen that queries backlinks.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object TestNetworkModule {

    @Provides
    @Singleton
    fun provideConstellationClient(): ConstellationClient = ConstellationClient

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(OfflineInterceptor)
        .build()

    private object OfflineInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response = Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(503)
            .message("Offline: instrumentation tests do not reach the network")
            .body("""{"error":"Offline","message":"Instrumentation tests do not reach the network"}"""
                .toResponseBody("application/json".toMediaType()))
            .build()
    }
}
