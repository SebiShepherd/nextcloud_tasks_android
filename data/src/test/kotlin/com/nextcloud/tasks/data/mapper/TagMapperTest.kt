package com.nextcloud.tasks.data.mapper

import com.nextcloud.tasks.data.api.dto.TagDto
import com.nextcloud.tasks.data.database.entity.TagEntity
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class TagMapperTest {
    private val mapper = TagMapper()

    @Test
    fun `toEntity maps all fields correctly`() {
        val dto =
            TagDto(
                id = "tag-123",
                name = "Work",
                updatedAt = 1640000000000L,
            )

        val entity = mapper.toEntity(dto)

        assertEquals("tag-123", entity.id)
        assertEquals("Work", entity.name)
        assertEquals(Instant.ofEpochMilli(1640000000000L), entity.updatedAt)
    }

    @Test
    fun `toDomain maps all fields correctly`() {
        val entity =
            TagEntity(
                id = "tag-456",
                name = "Personal",
                updatedAt = Instant.ofEpochMilli(1650000000000L),
            )

        val domain = mapper.toDomain(entity)

        assertEquals("tag-456", domain.id)
        assertEquals("Personal", domain.name)
        assertEquals(Instant.ofEpochMilli(1650000000000L), domain.updatedAt)
    }

    @Test
    fun `toEntity and toDomain round trip preserves data`() {
        val dto =
            TagDto(
                id = "tag-789",
                name = "Important",
                updatedAt = 1660000000000L,
            )

        val entity = mapper.toEntity(dto)
        val domain = mapper.toDomain(entity)

        assertEquals(dto.id, domain.id)
        assertEquals(dto.name, domain.name)
        assertEquals(Instant.ofEpochMilli(dto.updatedAt), domain.updatedAt)
    }
}
