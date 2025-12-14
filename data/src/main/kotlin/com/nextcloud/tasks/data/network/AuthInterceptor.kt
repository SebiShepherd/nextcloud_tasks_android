package com.nextcloud.tasks.data.network

import com.nextcloud.tasks.domain.model.AuthType
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(private val accountProvider: ActiveAccountProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val account = accountProvider.current ?: return chain.proceed(chain.request())
        val requestBuilder = chain.request().newBuilder()

        when (account.authType) {
            AuthType.BASIC -> {
                val username = account.username.orEmpty()
                val password = account.appPassword.orEmpty()
                if (username.isNotBlank() && password.isNotBlank()) {
                    requestBuilder.header("Authorization", Credentials.basic(username, password))
                }
            }

            AuthType.OAUTH -> {
                val token = account.accessToken
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
