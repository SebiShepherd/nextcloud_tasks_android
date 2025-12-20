package com.nextcloud.tasks.domain.model

data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
    val listId: String,
    val lastModified: Long,
    val dueDate: Long? = null,
    val tags: List<Tag> = emptyList(),
)
