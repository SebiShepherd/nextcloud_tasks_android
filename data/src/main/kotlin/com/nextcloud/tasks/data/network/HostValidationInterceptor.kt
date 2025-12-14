package com.nextcloud.tasks.data.network

import java.io.IOException
import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class HostValidationInterceptor @Inject constructor(private val accountProvider: ActiveAccountProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val account = accountProvider.current ?: return chain.proceed(chain.request())
        val expectedHost = account.serverUrl.toHttpUrlOrNull()?.host ?: return chain.proceed(chain.request())
        val request = chain.request()
        if (!request.url.host.equals(expectedHost, ignoreCase = true)) {
            throw IOException("Blocked request to unexpected host")
        }
        return chain.proceed(request)
    }
}
