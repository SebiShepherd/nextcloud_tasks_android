package com.nextcloud.tasks.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class BaseUrlInterceptor @Inject constructor(private val accountProvider: ActiveAccountProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val account = accountProvider.current ?: return chain.proceed(chain.request())
        val serverUrl = account.serverUrl.toHttpUrlOrNull() ?: return chain.proceed(chain.request())
        val original = chain.request()
        val rewrittenUrl =
            original.url
                .newBuilder()
                .scheme(serverUrl.scheme)
                .host(serverUrl.host)
                .port(serverUrl.port)
                .build()

        val safeRequest = original.newBuilder().url(rewrittenUrl).build()
        return chain.proceed(safeRequest)
    }
}
