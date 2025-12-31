package com.nextcloud.tasks.data.caldav.generator

import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VTodoGeneratorTest {
    private val generator = VTodoGenerator()

    @Test
    fun `generateVTodo with basic task generates valid VCALENDAR`() {
        val task = Task(
            id = "task-123",
            listId = "list-456",
            title = "Buy groceries",
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(ical.contains("BEGIN:VCALENDAR"))
        assertTrue(ical.contains("END:VCALENDAR"))
        assertTrue(ical.contains("VERSION:2.0"))
        assertTrue(ical.contains("PRODID:-//Nextcloud Tasks Android//EN"))
        assertTrue(ical.contains("BEGIN:VTODO"))
        assertTrue(ical.contains("END:VTODO"))
        assertTrue(ical.contains("SUMMARY:Buy groceries"))
        assertTrue(ical.contains("DTSTAMP"))
    }

    @Test
    fun `generateVTodo with existing UID preserves UID`() {
        val task = Task(
            id = "task-789",
            listId = "list-123",
            title = "Task with UID",
            updatedAt = Instant.now(),
            uid = "existing-uid-123",
        )

        val ical = generator.generateVTodo(task)

        assertTrue(ical.contains("UID:existing-uid-123"))
    }

    @Test
    fun `generateVTodo without UID generates new UID`() {
        val task = Task(
            id = "task-456",
            listId = "list-789",
            title = "Task without UID",
            updatedAt = Instant.now(),
            uid = null,
        )

        val ical = generator.generateVTodo(task)

        // Should contain UID property (UUID format)
        assertTrue(ical.contains("UID:"))
        // UUID pattern check (basic)
        val uidMatch = Regex("UID:([a-f0-9-]+)").find(ical)
        assertTrue(uidMatch != null)
    }

    @Test
    fun `generateVTodo with description includes DESCRIPTION property`() {
        val task = Task(
            id = "task-111",
            listId = "list-222",
            title = "Task with description",
            description = "This is a detailed description",
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(ical.contains("DESCRIPTION:This is a detailed description"))
    }

    @Test
    fun `generateVTodo without description excludes DESCRIPTION property`() {
        val task = Task(
            id = "task-222",
            listId = "list-333",
            title = "Task without description",
            description = null,
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(!ical.contains("DESCRIPTION:"))
    }

    @Test
    fun `generateVTodo with completed task includes STATUS COMPLETED`() {
        val completedAt = Instant.ofEpochMilli(1670000000000L)
        val task = Task(
            id = "task-333",
            listId = "list-444",
            title = "Completed task",
            completed = true,
            completedAt = completedAt,
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(ical.contains("STATUS:COMPLETED"))
        assertTrue(ical.contains("COMPLETED:"))
    }

    @Test
    fun `generateVTodo with incomplete task includes STATUS NEEDS-ACTION`() {
        val task = Task(
            id = "task-444",
            listId = "list-555",
            title = "Incomplete task",
            completed = false,
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(ical.contains("STATUS:NEEDS-ACTION"))
        assertTrue(!ical.contains("COMPLETED:"))
    }

    @Test
    fun `generateVTodo with priority includes PRIORITY property`() {
        val task = Task(
            id = "task-555",
            listId = "list-666",
            title = "High priority task",
            priority = 1,
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(ical.contains("PRIORITY:1"))
    }

    @Test
    fun `generateVTodo without priority excludes PRIORITY property`() {
        val task = Task(
            id = "task-666",
            listId = "list-777",
            title = "Task without priority",
            priority = null,
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(!ical.contains("PRIORITY:"))
    }

    @Test
    fun `generateVTodo with due date includes DUE property`() {
        val due = Instant.ofEpochMilli(1700000000000L)
        val task = Task(
            id = "task-777",
            listId = "list-888",
            title = "Task with due date",
            due = due,
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(ical.contains("DUE:"))
    }

    @Test
    fun `generateVTodo without due date excludes DUE property`() {
        val task = Task(
            id = "task-888",
            listId = "list-999",
            title = "Task without due date",
            due = null,
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(!ical.contains("DUE:"))
    }

    @Test
    fun `generateVTodo with tags includes CATEGORIES property`() {
        val task = Task(
            id = "task-999",
            listId = "list-111",
            title = "Task with tags",
            tags = listOf(
                Tag(id = "tag-1", name = "Work", updatedAt = Instant.now()),
                Tag(id = "tag-2", name = "Urgent", updatedAt = Instant.now()),
            ),
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(ical.contains("CATEGORIES:"))
        assertTrue(ical.contains("Work"))
        assertTrue(ical.contains("Urgent"))
    }

    @Test
    fun `generateVTodo without tags excludes CATEGORIES property`() {
        val task = Task(
            id = "task-000",
            listId = "list-000",
            title = "Task without tags",
            tags = emptyList(),
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(!ical.contains("CATEGORIES:"))
    }

    @Test
    fun `generateVTodo with parent UID includes RELATED-TO property`() {
        val task = Task(
            id = "subtask-123",
            listId = "list-456",
            title = "Subtask",
            parentUid = "parent-task-789",
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(ical.contains("RELATED-TO:parent-task-789"))
    }

    @Test
    fun `generateVTodo without parent UID excludes RELATED-TO property`() {
        val task = Task(
            id = "task-123",
            listId = "list-456",
            title = "Regular task",
            parentUid = null,
            updatedAt = Instant.now(),
        )

        val ical = generator.generateVTodo(task)

        assertTrue(!ical.contains("RELATED-TO:"))
    }

    @Test
    fun `generateVTodo with all fields generates complete VTODO`() {
        val task = Task(
            id = "complex-task",
            listId = "list-complex",
            title = "Complex task",
            description = "Detailed description",
            completed = true,
            due = Instant.ofEpochMilli(1700000000000L),
            updatedAt = Instant.now(),
            tags = listOf(
                Tag(id = "tag-1", name = "Work", updatedAt = Instant.now()),
                Tag(id = "tag-2", name = "Important", updatedAt = Instant.now()),
            ),
            priority = 1,
            completedAt = Instant.ofEpochMilli(1680000000000L),
            uid = "complex-uid-123",
            parentUid = "parent-uid-456",
        )

        val ical = generator.generateVTodo(task)

        // VCALENDAR structure
        assertTrue(ical.contains("BEGIN:VCALENDAR"))
        assertTrue(ical.contains("VERSION:2.0"))
        assertTrue(ical.contains("PRODID:-//Nextcloud Tasks Android//EN"))

        // VTODO structure
        assertTrue(ical.contains("BEGIN:VTODO"))
        assertTrue(ical.contains("END:VTODO"))

        // All properties
        assertTrue(ical.contains("UID:complex-uid-123"))
        assertTrue(ical.contains("SUMMARY:Complex task"))
        assertTrue(ical.contains("DESCRIPTION:Detailed description"))
        assertTrue(ical.contains("STATUS:COMPLETED"))
        assertTrue(ical.contains("COMPLETED:"))
        assertTrue(ical.contains("PRIORITY:1"))
        assertTrue(ical.contains("DUE:"))
        assertTrue(ical.contains("CATEGORIES:"))
        assertTrue(ical.contains("Work"))
        assertTrue(ical.contains("Important"))
        assertTrue(ical.contains("RELATED-TO:parent-uid-456"))

        // End of VCALENDAR
        assertTrue(ical.contains("END:VCALENDAR"))
    }

    @Test
    fun `generateFilename generates correct ICS filename`() {
        val uid = "test-uid-123"

        val filename = generator.generateFilename(uid)

        assertEquals("test-uid-123.ics", filename)
    }

    @Test
    fun `generateFilename with complex UID generates valid filename`() {
        val uid = "complex-uid-456-abc-def"

        val filename = generator.generateFilename(uid)

        assertEquals("complex-uid-456-abc-def.ics", filename)
    }
}
