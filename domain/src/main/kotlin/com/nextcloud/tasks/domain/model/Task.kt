package com.nextcloud.tasks.domain.model

import java.time.Instant

data class Task(
    val id: String,
    val listId: String,
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
    val due: Instant? = null,
    val updatedAt: Instant,
    val tags: List<Tag> = emptyList(),
    val priority: Int? = null,
    val status: String? = null,
    val completedAt: Instant? = null,
    val uid: String? = null,
    val etag: String? = null,
    val href: String? = null,
)
