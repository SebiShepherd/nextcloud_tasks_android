package com.nextcloud.tasks.domain.model

data class Account(
    val id: String,
    val serverUrl: String,
    val displayName: String,
    val username: String?,
    val authType: AuthType,
    val accessToken: String?,
    val refreshToken: String?,
    val appPassword: String?,
)
