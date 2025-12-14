package com.nextcloud.tasks.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextcloudClientFactory
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val moshiConverterFactory: MoshiConverterFactory,
    ) {
        fun create(serverUrl: String): NextcloudService = build(serverUrl, okHttpClient)

        fun createWithBasicAuth(serverUrl: String, username: String, password: String): NextcloudService {
            val client = okHttpClient.newBuilder().addInterceptor(BasicAuthInterceptor(username, password)).build()
            return build(serverUrl, client)
        }

        fun createWithBearerToken(serverUrl: String, token: String): NextcloudService {
            val client = okHttpClient.newBuilder().addInterceptor(BearerTokenInterceptor(token)).build()
            return build(serverUrl, client)
        }

        private fun build(serverUrl: String, client: OkHttpClient): NextcloudService {
            val normalizedBaseUrl = if (serverUrl.endsWith('/')) serverUrl else "$serverUrl/"
            return Retrofit.Builder()
                .client(client)
                .baseUrl(normalizedBaseUrl)
                .addConverterFactory(moshiConverterFactory)
                .build()
                .create(NextcloudService::class.java)
        }
    }
