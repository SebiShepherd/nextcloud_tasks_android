package com.nextcloud.tasks.data.auth

import com.nextcloud.tasks.domain.model.AuthType

sealed class AuthToken(open val serverUrl: String) {
    data class Password(
        override val serverUrl: String,
        val username: String,
        val appPassword: String,
    ) : AuthToken(serverUrl)

    data class OAuth(
        override val serverUrl: String,
        val accessToken: String,
        val refreshToken: String?,
    ) : AuthToken(serverUrl)

    fun authType(): AuthType = when (this) {
        is OAuth -> AuthType.OAUTH
        is Password -> AuthType.PASSWORD
    }
}
