package com.nextcloud.tasks.data.mapper

import com.nextcloud.tasks.data.api.dto.TagDto
import com.nextcloud.tasks.data.database.entity.TagEntity
import com.nextcloud.tasks.domain.model.Tag
import java.time.Instant
import javax.inject.Inject

class TagMapper @Inject constructor() {
    fun toEntity(dto: TagDto): TagEntity {
        return TagEntity(
            id = dto.id,
            name = dto.name,
            updatedAt = Instant.ofEpochMilli(dto.updatedAt),
        )
    }

    fun toDomain(entity: TagEntity): Tag {
        return Tag(
            id = entity.id,
            name = entity.name,
            updatedAt = entity.updatedAt,
        )
    }
}
