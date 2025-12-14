package com.nextcloud.tasks.data.network

import com.nextcloud.tasks.data.auth.AuthToken
import com.nextcloud.tasks.data.auth.AuthTokenProvider
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.activeToken()
        val requestBuilder =
            chain
                .request()
                .newBuilder()
                .header("OCS-APIREQUEST", "true")

        token?.let { attachAuthHeader(requestBuilder, it) }

        return chain.proceed(requestBuilder.build())
    }

    private fun attachAuthHeader(builder: okhttp3.Request.Builder, token: AuthToken) {
        when (token) {
            is AuthToken.OAuth -> builder.header("Authorization", "Bearer ${token.accessToken}")
            is AuthToken.Password -> builder.header("Authorization", Credentials.basic(token.username, token.appPassword))
        }
    }
}

class BasicAuthInterceptor(
    private val username: String,
    private val password: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain
                .request()
                .newBuilder()
                .header("Authorization", Credentials.basic(username, password))
                .header("OCS-APIREQUEST", "true")
                .build()
        return chain.proceed(request)
    }
}

class BearerTokenInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain
                .request()
                .newBuilder()
                .header("Authorization", "Bearer $token")
                .header("OCS-APIREQUEST", "true")
                .build()
        return chain.proceed(request)
    }
}
