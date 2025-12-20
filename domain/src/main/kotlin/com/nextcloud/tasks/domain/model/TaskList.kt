package com.nextcloud.tasks.domain.model

import java.time.Instant

data class TaskList(
    val id: String,
    val name: String,
    val color: String? = null,
    val updatedAt: Instant,
)
