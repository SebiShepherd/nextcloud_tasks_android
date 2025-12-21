package com.nextcloud.tasks.data.api

import com.nextcloud.tasks.data.auth.AuthTokenProvider
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class NextcloudTasksApiFactory
    @Inject
    constructor(
        @Named("authenticated") private val okHttpClient: OkHttpClient,
        private val moshiConverterFactory: MoshiConverterFactory,
        private val authTokenProvider: AuthTokenProvider,
    ) {
        fun create(): NextcloudTasksApi {
            val serverUrl =
                requireNotNull(authTokenProvider.activeServerUrl()) {
                    "Active server URL is missing; ensure an account is selected."
                }
            val normalized = if (serverUrl.endsWith('/')) serverUrl else "$serverUrl/"
            return Retrofit
                .Builder()
                .client(okHttpClient)
                .baseUrl(normalized)
                .addConverterFactory(moshiConverterFactory)
                .build()
                .create(NextcloudTasksApi::class.java)
        }
    }
