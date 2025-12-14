package com.nextcloud.tasks.domain.model

data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
)
