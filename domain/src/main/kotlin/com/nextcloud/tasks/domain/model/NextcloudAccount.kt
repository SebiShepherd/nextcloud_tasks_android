package com.nextcloud.tasks.domain.model

data class NextcloudAccount(
    val id: String,
    val displayName: String,
    val serverUrl: String,
    val username: String,
    val authType: AuthType,
)
