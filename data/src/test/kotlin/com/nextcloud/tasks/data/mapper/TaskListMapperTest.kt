package com.nextcloud.tasks.data.mapper

import com.nextcloud.tasks.data.api.dto.TaskListDto
import com.nextcloud.tasks.data.database.entity.TaskListEntity
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskListMapperTest {
    private val mapper = TaskListMapper()

    @Test
    fun `toEntity maps all fields correctly with color`() {
        val dto = TaskListDto(
            id = "list-123",
            name = "Work Tasks",
            color = "#FF5733",
            updatedAt = 1640000000000L,
        )

        val entity = mapper.toEntity(dto, accountId = "account-456")

        assertEquals("list-123", entity.id)
        assertEquals("account-456", entity.accountId)
        assertEquals("Work Tasks", entity.name)
        assertEquals("#FF5733", entity.color)
        assertEquals(Instant.ofEpochMilli(1640000000000L), entity.updatedAt)
        assertNull(entity.etag)
        assertNull(entity.href)
        assertNull(entity.order)
    }

    @Test
    fun `toEntity maps correctly with null color`() {
        val dto = TaskListDto(
            id = "list-789",
            name = "Personal Tasks",
            color = null,
            updatedAt = 1650000000000L,
        )

        val entity = mapper.toEntity(dto, accountId = "account-123")

        assertEquals("list-789", entity.id)
        assertEquals("account-123", entity.accountId)
        assertEquals("Personal Tasks", entity.name)
        assertNull(entity.color)
        assertEquals(Instant.ofEpochMilli(1650000000000L), entity.updatedAt)
    }

    @Test
    fun `toDomain maps all fields correctly`() {
        val entity = TaskListEntity(
            id = "list-456",
            accountId = "account-789",
            name = "Shopping",
            color = "#00FF00",
            updatedAt = Instant.ofEpochMilli(1660000000000L),
            etag = "etag-123",
            href = "/calendars/user/tasks/",
            order = 1,
        )

        val domain = mapper.toDomain(entity)

        assertEquals("list-456", domain.id)
        assertEquals("Shopping", domain.name)
        assertEquals("#00FF00", domain.color)
        assertEquals(Instant.ofEpochMilli(1660000000000L), domain.updatedAt)
        assertEquals("etag-123", domain.etag)
        assertEquals("/calendars/user/tasks/", domain.href)
        assertEquals(1, domain.order)
    }

    @Test
    fun `toDomain maps correctly with null optional fields`() {
        val entity = TaskListEntity(
            id = "list-999",
            accountId = "account-111",
            name = "Simple List",
            color = null,
            updatedAt = Instant.ofEpochMilli(1670000000000L),
            etag = null,
            href = null,
            order = null,
        )

        val domain = mapper.toDomain(entity)

        assertEquals("list-999", domain.id)
        assertEquals("Simple List", domain.name)
        assertNull(domain.color)
        assertEquals(Instant.ofEpochMilli(1670000000000L), domain.updatedAt)
        assertNull(domain.etag)
        assertNull(domain.href)
        assertNull(domain.order)
    }
}
