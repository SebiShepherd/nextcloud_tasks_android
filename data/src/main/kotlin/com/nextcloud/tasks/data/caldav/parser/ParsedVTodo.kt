package com.nextcloud.tasks.data.caldav.parser

import com.nextcloud.tasks.data.database.entity.TaskEntity

data class ParsedVTodo(
    val entity: TaskEntity,
    val categories: List<String>,
)
