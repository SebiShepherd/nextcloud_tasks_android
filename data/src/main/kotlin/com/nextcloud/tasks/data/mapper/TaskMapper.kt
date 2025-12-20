package com.nextcloud.tasks.data.mapper

import com.nextcloud.tasks.data.api.dto.TaskDto
import com.nextcloud.tasks.data.api.dto.TaskRequestDto
import com.nextcloud.tasks.data.database.entity.TaskEntity
import com.nextcloud.tasks.data.database.entity.TaskTagCrossRef
import com.nextcloud.tasks.data.database.model.TaskWithRelations
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskDraft
import java.time.Instant
import javax.inject.Inject

class TaskMapper
    @Inject
    constructor(
        private val tagMapper: TagMapper,
    ) {
        fun toEntity(dto: TaskDto): TaskEntity =
            TaskEntity(
                id = dto.id,
                listId = dto.listId,
                title = dto.title,
                description = dto.description,
                completed = dto.completed,
                due = dto.due?.let { Instant.ofEpochMilli(it) },
                updatedAt = Instant.ofEpochMilli(dto.updatedAt),
            )

        fun toDomain(taskWithRelations: TaskWithRelations): Task {
            val tags = taskWithRelations.tags.map(tagMapper::toDomain)
            return Task(
                id = taskWithRelations.task.id,
                listId = taskWithRelations.task.listId,
                title = taskWithRelations.task.title,
                description = taskWithRelations.task.description,
                completed = taskWithRelations.task.completed,
                due = taskWithRelations.task.due,
                updatedAt = taskWithRelations.task.updatedAt,
                tags = tags,
            )
        }

        fun toRequest(task: Task): TaskRequestDto =
            TaskRequestDto(
                listId = task.listId,
                title = task.title,
                description = task.description,
                completed = task.completed,
                due = task.due?.toEpochMilli(),
                tagIds = task.tags.map(Tag::id),
                updatedAt = task.updatedAt.toEpochMilli(),
            )

        fun toRequest(
            draft: TaskDraft,
            updatedAt: Instant,
        ): TaskRequestDto =
            TaskRequestDto(
                listId = draft.listId,
                title = draft.title,
                description = draft.description,
                completed = draft.completed,
                due = draft.due?.toEpochMilli(),
                tagIds = draft.tagIds,
                updatedAt = updatedAt.toEpochMilli(),
            )

        fun crossRefs(
            taskId: String,
            tagIds: List<String>,
        ): List<TaskTagCrossRef> =
            tagIds.map { tagId ->
                TaskTagCrossRef(
                    taskId = taskId,
                    tagId = tagId,
                )
            }
    }
