package com.nextcloud.tasks.data.di

import com.nextcloud.tasks.data.network.ActiveAccountProvider
import com.nextcloud.tasks.data.network.AuthInterceptor
import com.nextcloud.tasks.data.network.BaseUrlInterceptor
import com.nextcloud.tasks.data.network.HostValidationInterceptor
import com.nextcloud.tasks.data.network.NextcloudApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val PLACEHOLDER_BASE_URL = "https://placeholder.invalid/"

    @Provides
    @Singleton
    fun provideOkHttpClient(
        baseUrlInterceptor: BaseUrlInterceptor,
        authInterceptor: AuthInterceptor,
        hostValidationInterceptor: HostValidationInterceptor,
        activeAccountProvider: ActiveAccountProvider,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val hostnameVerifier =
            javax.net.ssl.HostnameVerifier { hostname, _ ->
                val expectedHost = activeAccountProvider.current?.serverUrl?.substringAfter("//")?.substringBefore("/")
                expectedHost == null || hostname.equals(expectedHost, ignoreCase = true)
            }

        return OkHttpClient.Builder()
            .addInterceptor(hostValidationInterceptor)
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .hostnameVerifier(hostnameVerifier)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideNextcloudApi(retrofit: Retrofit): NextcloudApi = retrofit.create(NextcloudApi::class.java)
}
