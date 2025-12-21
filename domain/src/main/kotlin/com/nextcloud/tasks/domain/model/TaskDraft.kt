package com.nextcloud.tasks.domain.model

import java.time.Instant

data class TaskDraft(
    val listId: String,
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
    val priority: Int = 0,
    val due: Instant? = null,
    val tagIds: List<String> = emptyList(),
)
