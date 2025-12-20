package com.nextcloud.tasks.data.di

import com.nextcloud.tasks.data.BuildConfig
import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.network.AuthInterceptor
import com.nextcloud.tasks.data.network.SafeDns
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Provides
    @Singleton
    fun provideMoshiConverterFactory(moshi: Moshi): MoshiConverterFactory = MoshiConverterFactory.create(moshi)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            level =
                if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
        }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenProvider: AuthTokenProvider): AuthInterceptor = AuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideSafeDns(): Dns = SafeDns()

    @Provides
    @Singleton
    @Named("unauthenticated")
    fun provideUnauthenticatedOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        dns: Dns,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .dns(dns)
            .apply {
                if (loggingInterceptor.level != HttpLoggingInterceptor.Level.NONE) {
                    addInterceptor(loggingInterceptor)
                }
            }.build()

    @Named("authenticated")
    @Singleton
    @Provides
    fun provideAuthenticatedOkHttpClient(
        @Named("unauthenticated") baseClient: OkHttpClient,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient = baseClient.newBuilder().addInterceptor(authInterceptor).build()
}
