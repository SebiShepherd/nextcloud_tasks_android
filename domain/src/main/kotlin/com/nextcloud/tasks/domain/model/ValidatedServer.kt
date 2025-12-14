package com.nextcloud.tasks.domain.model

data class ValidatedServer(
    val normalizedUrl: String,
    val isHttps: Boolean,
)
