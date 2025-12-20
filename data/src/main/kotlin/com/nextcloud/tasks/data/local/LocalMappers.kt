package com.nextcloud.tasks.data.local

import com.nextcloud.tasks.data.local.dao.TaskWithTagsPayload
import com.nextcloud.tasks.data.local.entity.TagEntity
import com.nextcloud.tasks.data.local.entity.TaskEntity
import com.nextcloud.tasks.data.local.entity.TaskListEntity
import com.nextcloud.tasks.data.local.entity.TaskWithRelations
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskList

internal fun TaskWithRelations.toDomain(): Task =
    Task(
        id = task.id,
        title = task.title,
        description = task.description,
        completed = task.completed,
        listId = task.listId,
        lastModified = task.lastModified,
        dueDate = task.dueDate,
        tags = tags.map { it.toDomain() },
    )

internal fun TaskListEntity.toDomain(): TaskList =
    TaskList(
        id = id,
        name = name,
        color = color,
        lastModified = lastModified,
    )

internal fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, color = color)

internal fun Task.toEntity(): TaskEntity =
    TaskEntity(
        id = id,
        title = title,
        description = description,
        completed = completed,
        listId = listId,
        lastModified = lastModified,
        dueDate = dueDate,
    )

internal fun Task.tagsToEntities(): List<TagEntity> = tags.map { TagEntity(id = it.id, name = it.name, color = it.color) }

internal fun Task.toPayload(): TaskWithTagsPayload =
    TaskWithTagsPayload(
        task = toEntity(),
        tags = tagsToEntities(),
    )

internal fun TaskList.toEntity(): TaskListEntity =
    TaskListEntity(
        id = id,
        name = name,
        color = color,
        lastModified = lastModified,
    )
