package com.nextcloud.tasks.data.di

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import com.nextcloud.tasks.data.BuildConfig
import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.network.AppForegroundMonitor
import com.nextcloud.tasks.data.network.AuthInterceptor
import com.nextcloud.tasks.data.network.SafeDns
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.SSLContext

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
        @ApplicationContext context: Context,
        loggingInterceptor: HttpLoggingInterceptor,
        dns: Dns,
        foregroundMonitor: AppForegroundMonitor,
    ): OkHttpClient {
        // cert4android trust manager: validates against the system trust store and additionally
        // lets the user explicitly accept self-signed / custom-CA / wrong-hostname certificates
        // (trust on first use). Built inline rather than provided as a Hilt binding so the AAR-only
        // CustomCertManager type never leaks into a generated Hilt factory signature (which the
        // javac step compiling those factories cannot resolve). All other clients derive from this
        // base client via newBuilder(), so login and sync share the same trust handling.
        val certManager =
            CustomCertManager(
                context = context,
                trustSystemCerts = true,
                appInForeground = foregroundMonitor.isInForeground,
            )
        val sslContext =
            SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(certManager), null)
            }
        return OkHttpClient
            .Builder()
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .dns(dns)
            .sslSocketFactory(sslContext.socketFactory, certManager)
            .hostnameVerifier(certManager.HostnameVerifier(OkHostnameVerifier))
            .apply {
                if (loggingInterceptor.level != HttpLoggingInterceptor.Level.NONE) {
                    addInterceptor(loggingInterceptor)
                }
            }.build()
    }

    @Named("authenticated")
    @Singleton
    @Provides
    fun provideAuthenticatedOkHttpClient(
        @Named("unauthenticated") baseClient: OkHttpClient,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient = baseClient.newBuilder().addInterceptor(authInterceptor).build()
}
