package com.nextcloud.tasks.domain.model

data class Sharee(
    val id: String,
    val displayName: String,
    val access: ShareAccess,
    val type: ShareeType,
)
