package com.nextcloud.tasks.domain.model

data class TaskList(
    val id: String,
    val name: String,
    val color: String? = null,
    val lastModified: Long,
)
