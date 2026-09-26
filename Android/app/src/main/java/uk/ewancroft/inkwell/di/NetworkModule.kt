package uk.ewancroft.inkwell.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import uk.ewancroft.inkwell.data.remote.ConstellationClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideConstellationClient(): ConstellationClient = ConstellationClient

    /** The one shared, configured OkHttpClient — see [SharedHttpClient]. */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = SharedHttpClient.client
}
