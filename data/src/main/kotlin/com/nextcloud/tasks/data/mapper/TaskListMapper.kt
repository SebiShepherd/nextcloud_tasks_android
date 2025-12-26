package com.nextcloud.tasks.data.mapper

import com.nextcloud.tasks.data.api.dto.TaskListDto
import com.nextcloud.tasks.data.database.entity.TaskListEntity
import com.nextcloud.tasks.domain.model.TaskList
import java.time.Instant
import javax.inject.Inject

class TaskListMapper
    @Inject
    constructor() {
        fun toEntity(
            dto: TaskListDto,
            accountId: String,
        ): TaskListEntity =
            TaskListEntity(
                id = dto.id,
                accountId = accountId,
                name = dto.name,
                color = dto.color,
                updatedAt = Instant.ofEpochMilli(dto.updatedAt),
                etag = null,
                href = null,
                order = null,
            )

        fun toDomain(entity: TaskListEntity): TaskList =
            TaskList(
                id = entity.id,
                name = entity.name,
                color = entity.color,
                updatedAt = entity.updatedAt,
                etag = entity.etag,
                href = entity.href,
                order = entity.order,
            )
    }
