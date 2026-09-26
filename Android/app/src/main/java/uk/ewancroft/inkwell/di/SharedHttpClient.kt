package uk.ewancroft.inkwell.di

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * The single, properly-configured [OkHttpClient] the app is meant to reuse
 * everywhere it talks HTTP — PDS/XRPC calls, Constellation, the public
 * Bluesky API, Discover search, and embed fetching all share one connection
 * pool and dispatcher instead of each call site standing up its own
 * unconfigured client.
 *
 * Exposed both as a plain object (for `object`-scoped callers that can't
 * take a Hilt-injected constructor parameter, e.g. [uk.ewancroft.inkwell.data.remote.BSkyListFetcher])
 * and via [uk.ewancroft.inkwell.di.NetworkModule.provideOkHttpClient] (for
 * Hilt-constructed classes, which should prefer constructor injection over
 * referencing this object directly).
 */
object SharedHttpClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
