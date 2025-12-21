package com.nextcloud.tasks.domain.model

import java.time.Instant

data class Task(
    val id: String,
    val listId: String,
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
    val priority: Int = 0,
    val due: Instant? = null,
    val updatedAt: Instant,
    val tags: List<Tag> = emptyList(),
)
