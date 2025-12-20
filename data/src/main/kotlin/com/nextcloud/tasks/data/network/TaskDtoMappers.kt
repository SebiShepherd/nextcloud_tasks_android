package com.nextcloud.tasks.data.network

import com.nextcloud.tasks.data.network.model.TagDto
import com.nextcloud.tasks.data.network.model.TaskDto
import com.nextcloud.tasks.data.network.model.TaskListDto
import com.nextcloud.tasks.data.network.model.TaskRequest
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskList

internal fun TaskDto.toDomain(): Task =
    Task(
        id = id,
        title = title,
        description = description,
        completed = completed,
        listId = listId,
        lastModified = lastModified,
        dueDate = dueDate,
        tags = tags.map { it.toDomain() },
    )

internal fun TaskListDto.toDomain(): TaskList =
    TaskList(
        id = id,
        name = name,
        color = color,
        lastModified = lastModified,
    )

internal fun TagDto.toDomain(): Tag = Tag(id = id, name = name, color = color)

internal fun Task.toRequest(): TaskRequest =
    TaskRequest(
        title = title,
        description = description,
        completed = completed,
        listId = listId,
        dueDate = dueDate,
        tags = tags.map { it.id },
    )
