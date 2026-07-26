package com.nextcloud.tasks.data.di

import com.nextcloud.tasks.data.BuildConfig
import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.network.AppCertStore
import com.nextcloud.tasks.data.network.AppCertTrustManager
import com.nextcloud.tasks.data.network.AppForegroundMonitor
import com.nextcloud.tasks.data.network.AppHostnameVerifier
import com.nextcloud.tasks.data.network.AuthInterceptor
import com.nextcloud.tasks.data.network.CertDecisionRegistry
import com.nextcloud.tasks.data.network.ReloadableSslSocketFactory
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
import java.security.KeyStore
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

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

    // Trust manager that validates against the system trust store first and, only on failure,
    // lets the user explicitly accept self-signed / custom-CA / wrong-hostname certificates
    // (trust on first use).
    @Provides
    @Singleton
    fun provideAppCertTrustManager(
        certStore: AppCertStore,
        certRegistry: CertDecisionRegistry,
        foregroundMonitor: AppForegroundMonitor,
    ): AppCertTrustManager {
        val systemTrustManager =
            TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm())
                .apply { init(null as KeyStore?) }
                .trustManagers
                .filterIsInstance<X509TrustManager>()
                .firstOrNull()
                ?: error("No system X509TrustManager available")
        return AppCertTrustManager(
            systemTrustManager = systemTrustManager,
            store = certStore,
            registry = certRegistry,
            appInForeground = foregroundMonitor.isInForeground,
        )
    }

    // Swappable socket factory so CertTrustReset can install a fresh SSLContext (empty TLS session
    // cache) and force a full handshake — and thus certificate re-validation — after a reset.
    @Provides
    @Singleton
    fun provideReloadableSslSocketFactory(trustManager: AppCertTrustManager): ReloadableSslSocketFactory {
        val sslContext =
            SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<javax.net.ssl.TrustManager>(trustManager), null)
            }
        return ReloadableSslSocketFactory(sslContext.socketFactory)
    }

    @Provides
    @Singleton
    @Named("unauthenticated")
    fun provideUnauthenticatedOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        dns: Dns,
        certStore: AppCertStore,
        trustManager: AppCertTrustManager,
        sslSocketFactory: ReloadableSslSocketFactory,
    ): OkHttpClient {
        // All other clients derive from this base via newBuilder(), so login and sync share the
        // same trust handling and connection pool.
        return OkHttpClient
            .Builder()
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .dns(dns)
            .sslSocketFactory(sslSocketFactory, trustManager)
            .hostnameVerifier(AppHostnameVerifier(certStore))
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
