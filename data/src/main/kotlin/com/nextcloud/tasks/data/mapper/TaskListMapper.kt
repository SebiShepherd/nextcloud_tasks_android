package com.nextcloud.tasks.data.mapper

import com.nextcloud.tasks.data.api.dto.TaskListDto
import com.nextcloud.tasks.data.database.entity.TaskListEntity
import com.nextcloud.tasks.domain.model.TaskList
import java.time.Instant
import javax.inject.Inject

class TaskListMapper @Inject constructor() {
    fun toEntity(dto: TaskListDto): TaskListEntity {
        return TaskListEntity(
            id = dto.id,
            name = dto.name,
            color = dto.color,
            updatedAt = Instant.ofEpochMilli(dto.updatedAt),
        )
    }

    fun toDomain(entity: TaskListEntity): TaskList {
        return TaskList(
            id = entity.id,
            name = entity.name,
            color = entity.color,
            updatedAt = entity.updatedAt,
        )
    }
}
