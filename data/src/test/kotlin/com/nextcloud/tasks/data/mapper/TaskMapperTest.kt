package com.nextcloud.tasks.data.mapper

import com.nextcloud.tasks.data.api.dto.TaskDto
import com.nextcloud.tasks.data.database.entity.TagEntity
import com.nextcloud.tasks.data.database.entity.TaskEntity
import com.nextcloud.tasks.data.database.model.TaskWithRelations
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskDraft
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskMapperTest {
    private val tagMapper = mockk<TagMapper>()
    private val mapper = TaskMapper(tagMapper)

    @Test
    fun `toEntity maps all fields correctly`() {
        val dto = TaskDto(
            id = "task-123",
            listId = "list-456",
            title = "Buy groceries",
            description = "Milk, bread, eggs",
            completed = false,
            due = 1640000000000L,
            updatedAt = 1650000000000L,
        )

        val entity = mapper.toEntity(dto, accountId = "account-789")

        assertEquals("task-123", entity.id)
        assertEquals("account-789", entity.accountId)
        assertEquals("list-456", entity.listId)
        assertEquals("Buy groceries", entity.title)
        assertEquals("Milk, bread, eggs", entity.description)
        assertEquals(false, entity.completed)
        assertEquals(Instant.ofEpochMilli(1640000000000L), entity.due)
        assertEquals(Instant.ofEpochMilli(1650000000000L), entity.updatedAt)
        assertNull(entity.priority)
        assertNull(entity.status)
        assertNull(entity.completedAt)
        assertNull(entity.uid)
        assertNull(entity.etag)
        assertNull(entity.href)
        assertNull(entity.parentUid)
    }

    @Test
    fun `toEntity maps correctly with null description and due`() {
        val dto = TaskDto(
            id = "task-456",
            listId = "list-789",
            title = "Simple task",
            description = null,
            completed = true,
            due = null,
            updatedAt = 1660000000000L,
        )

        val entity = mapper.toEntity(dto, accountId = "account-123")

        assertEquals("task-456", entity.id)
        assertEquals("Simple task", entity.title)
        assertNull(entity.description)
        assertEquals(true, entity.completed)
        assertNull(entity.due)
    }

    @Test
    fun `toDomain maps all fields correctly with tags`() {
        val tagEntity1 = TagEntity(
            id = "tag-1",
            name = "Work",
            updatedAt = Instant.now(),
        )
        val tagEntity2 = TagEntity(
            id = "tag-2",
            name = "Urgent",
            updatedAt = Instant.now(),
        )
        val tag1 = Tag(id = "tag-1", name = "Work", updatedAt = Instant.now())
        val tag2 = Tag(id = "tag-2", name = "Urgent", updatedAt = Instant.now())

        every { tagMapper.toDomain(tagEntity1) } returns tag1
        every { tagMapper.toDomain(tagEntity2) } returns tag2

        val taskEntity = TaskEntity(
            id = "task-789",
            accountId = "account-456",
            listId = "list-123",
            title = "Important meeting",
            description = "Discuss Q4 results",
            completed = false,
            due = Instant.ofEpochMilli(1670000000000L),
            updatedAt = Instant.ofEpochMilli(1660000000000L),
            priority = 1,
            status = "NEEDS-ACTION",
            completedAt = null,
            uid = "uid-123",
            etag = "etag-456",
            href = "/calendars/user/tasks/task-789.ics",
            parentUid = null,
        )

        val taskWithRelations = TaskWithRelations(
            task = taskEntity,
            list = null,
            tags = listOf(tagEntity1, tagEntity2),
        )

        val domain = mapper.toDomain(taskWithRelations)

        assertEquals("task-789", domain.id)
        assertEquals("list-123", domain.listId)
        assertEquals("Important meeting", domain.title)
        assertEquals("Discuss Q4 results", domain.description)
        assertEquals(false, domain.completed)
        assertEquals(Instant.ofEpochMilli(1670000000000L), domain.due)
        assertEquals(Instant.ofEpochMilli(1660000000000L), domain.updatedAt)
        assertEquals(2, domain.tags.size)
        assertEquals("Work", domain.tags[0].name)
        assertEquals("Urgent", domain.tags[1].name)
        assertEquals(1, domain.priority)
        assertEquals("NEEDS-ACTION", domain.status)
        assertNull(domain.completedAt)
        assertEquals("uid-123", domain.uid)
        assertEquals("etag-456", domain.etag)
        assertEquals("/calendars/user/tasks/task-789.ics", domain.href)
        assertNull(domain.parentUid)
    }

    @Test
    fun `toDomain maps correctly with no tags`() {
        val taskEntity = TaskEntity(
            id = "task-999",
            accountId = "account-111",
            listId = "list-222",
            title = "No tags task",
            description = null,
            completed = true,
            due = null,
            updatedAt = Instant.ofEpochMilli(1680000000000L),
            priority = null,
            status = "COMPLETED",
            completedAt = Instant.ofEpochMilli(1680000000000L),
            uid = "uid-999",
            etag = "etag-999",
            href = "/calendars/user/tasks/task-999.ics",
            parentUid = "parent-uid-123",
        )

        val taskWithRelations = TaskWithRelations(
            task = taskEntity,
            list = null,
            tags = emptyList(),
        )

        val domain = mapper.toDomain(taskWithRelations)

        assertEquals("task-999", domain.id)
        assertEquals("No tags task", domain.title)
        assertEquals(true, domain.completed)
        assertEquals(0, domain.tags.size)
        assertEquals("COMPLETED", domain.status)
        assertEquals(Instant.ofEpochMilli(1680000000000L), domain.completedAt)
        assertEquals("parent-uid-123", domain.parentUid)
    }

    @Test
    fun `toRequest with Task maps all fields correctly`() {
        val task = Task(
            id = "task-111",
            listId = "list-222",
            title = "Write tests",
            description = "Comprehensive test coverage",
            completed = false,
            due = Instant.ofEpochMilli(1690000000000L),
            updatedAt = Instant.ofEpochMilli(1685000000000L),
            tags = listOf(
                Tag(id = "tag-1", name = "Dev", updatedAt = Instant.now()),
                Tag(id = "tag-2", name = "Testing", updatedAt = Instant.now()),
            ),
            priority = 2,
            status = "IN-PROCESS",
        )

        val requestDto = mapper.toRequest(task)

        assertEquals("list-222", requestDto.listId)
        assertEquals("Write tests", requestDto.title)
        assertEquals("Comprehensive test coverage", requestDto.description)
        assertEquals(false, requestDto.completed)
        assertEquals(1690000000000L, requestDto.due)
        assertEquals(1685000000000L, requestDto.updatedAt)
        assertEquals(2, requestDto.tagIds.size)
        assertEquals("tag-1", requestDto.tagIds[0])
        assertEquals("tag-2", requestDto.tagIds[1])
    }

    @Test
    fun `toRequest with Task handles null fields`() {
        val task = Task(
            id = "task-222",
            listId = "list-333",
            title = "Simple task",
            description = null,
            completed = true,
            due = null,
            updatedAt = Instant.ofEpochMilli(1695000000000L),
            tags = emptyList(),
        )

        val requestDto = mapper.toRequest(task)

        assertEquals("list-333", requestDto.listId)
        assertEquals("Simple task", requestDto.title)
        assertNull(requestDto.description)
        assertEquals(true, requestDto.completed)
        assertNull(requestDto.due)
        assertEquals(1695000000000L, requestDto.updatedAt)
        assertEquals(0, requestDto.tagIds.size)
    }

    @Test
    fun `toRequest with TaskDraft maps all fields correctly`() {
        val draft = TaskDraft(
            listId = "list-444",
            title = "New draft task",
            description = "Draft description",
            completed = false,
            due = Instant.ofEpochMilli(1700000000000L),
            tagIds = listOf("tag-3", "tag-4"),
        )
        val updatedAt = Instant.ofEpochMilli(1698000000000L)

        val requestDto = mapper.toRequest(draft, updatedAt)

        assertEquals("list-444", requestDto.listId)
        assertEquals("New draft task", requestDto.title)
        assertEquals("Draft description", requestDto.description)
        assertEquals(false, requestDto.completed)
        assertEquals(1700000000000L, requestDto.due)
        assertEquals(1698000000000L, requestDto.updatedAt)
        assertEquals(2, requestDto.tagIds.size)
        assertEquals("tag-3", requestDto.tagIds[0])
        assertEquals("tag-4", requestDto.tagIds[1])
    }

    @Test
    fun `crossRefs creates correct TaskTagCrossRef list`() {
        val taskId = "task-555"
        val tagIds = listOf("tag-5", "tag-6", "tag-7")

        val crossRefs = mapper.crossRefs(taskId, tagIds)

        assertEquals(3, crossRefs.size)
        assertEquals("task-555", crossRefs[0].taskId)
        assertEquals("tag-5", crossRefs[0].tagId)
        assertEquals("task-555", crossRefs[1].taskId)
        assertEquals("tag-6", crossRefs[1].tagId)
        assertEquals("task-555", crossRefs[2].taskId)
        assertEquals("tag-7", crossRefs[2].tagId)
    }

    @Test
    fun `crossRefs with empty tagIds returns empty list`() {
        val taskId = "task-666"
        val tagIds = emptyList<String>()

        val crossRefs = mapper.crossRefs(taskId, tagIds)

        assertEquals(0, crossRefs.size)
    }
}
