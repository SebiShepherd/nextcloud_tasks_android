package com.nextcloud.tasks.domain.model

import java.time.Instant

data class Tag(
    val id: String,
    val name: String,
    val updatedAt: Instant,
)
