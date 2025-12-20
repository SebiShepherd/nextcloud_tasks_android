package com.nextcloud.tasks.domain.model

data class Tag(
    val id: String,
    val name: String,
    val color: String? = null,
)
