package com.nextcloud.tasks.data.caldav.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VTodoParserTest {
    private val parser = VTodoParser()

    private val accountId = "test-account"
    private val listId = "test-list"
    private val href = "/calendars/user/tasks/test.ics"
    private val etag = "test-etag"

    @Test
    fun `parseVTodo with valid basic VTODO returns TaskEntity`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:task-123
            SUMMARY:Buy groceries
            DTSTAMP:20231201T120000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val task = parser.parseVTodo(icalData, accountId, listId, href, etag)

        assertNotNull(task)
        assertEquals("task-123", task.id)
        assertEquals("task-123", task.uid)
        assertEquals(accountId, task.accountId)
        assertEquals(listId, task.listId)
        assertEquals("Buy groceries", task.title)
        assertNull(task.description)
        assertEquals(false, task.completed)
        assertEquals(href, task.href)
        assertEquals(etag, task.etag)
    }

    @Test
    fun `parseVTodo with description returns TaskEntity with description`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:task-456
            SUMMARY:Meeting
            DESCRIPTION:Discuss project timeline
            DTSTAMP:20231201T120000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val task = parser.parseVTodo(icalData, accountId, listId, href, etag)

        assertNotNull(task)
        assertEquals("Meeting", task.title)
        assertEquals("Discuss project timeline", task.description)
    }

    @Test
    fun `parseVTodo with COMPLETED status sets completed to true`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:task-789
            SUMMARY:Finished task
            STATUS:COMPLETED
            COMPLETED:20231201T120000Z
            DTSTAMP:20231201T100000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val task = parser.parseVTodo(icalData, accountId, listId, href, etag)

        assertNotNull(task)
        assertEquals(true, task.completed)
        assertEquals("COMPLETED", task.status)
        assertNotNull(task.completedAt)
    }

    @Test
    fun `parseVTodo with DUE date parses due correctly`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:task-999
            SUMMARY:Task with due date
            DUE:20231225T120000Z
            DTSTAMP:20231201T120000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val task = parser.parseVTodo(icalData, accountId, listId, href, etag)

        assertNotNull(task)
        assertNotNull(task.due)
    }

    @Test
    fun `parseVTodo with PRIORITY parses priority correctly`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:task-111
            SUMMARY:High priority task
            PRIORITY:1
            DTSTAMP:20231201T120000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val task = parser.parseVTodo(icalData, accountId, listId, href, etag)

        assertNotNull(task)
        assertEquals(1, task.priority)
    }

    @Test
    fun `parseVTodo with RELATED-TO parses parent UID correctly`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:subtask-123
            SUMMARY:Subtask
            RELATED-TO:parent-task-456
            DTSTAMP:20231201T120000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val task = parser.parseVTodo(icalData, accountId, listId, href, etag)

        assertNotNull(task)
        assertEquals("parent-task-456", task.parentUid)
    }

    @Test
    fun `parseVTodo with missing UID returns null`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            SUMMARY:Task without UID
            DTSTAMP:20231201T120000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val task = parser.parseVTodo(icalData, accountId, listId, href, etag)

        assertNull(task)
    }

    @Test
    fun `parseVTodo with malformed iCalendar returns null`() {
        val icalData = "This is not valid iCalendar data"

        val task = parser.parseVTodo(icalData, accountId, listId, href, etag)

        assertNull(task)
    }

    @Test
    fun `parseVTodo with empty string returns null`() {
        val icalData = ""

        val task = parser.parseVTodo(icalData, accountId, listId, href, etag)

        assertNull(task)
    }

    @Test
    fun `parseVTodos with single VTODO returns list with one task`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:task-single
            SUMMARY:Single task
            DTSTAMP:20231201T120000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val tasks = parser.parseVTodos(icalData, accountId, listId, href, etag)

        assertEquals(1, tasks.size)
        assertEquals("Single task", tasks[0].title)
    }

    @Test
    fun `parseVTodos with multiple VTODOs returns list with all tasks`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:task-1
            SUMMARY:First task
            DTSTAMP:20231201T120000Z
            END:VTODO
            BEGIN:VTODO
            UID:task-2
            SUMMARY:Second task
            DTSTAMP:20231201T120000Z
            END:VTODO
            BEGIN:VTODO
            UID:task-3
            SUMMARY:Third task
            DTSTAMP:20231201T120000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val tasks = parser.parseVTodos(icalData, accountId, listId, href, etag)

        assertEquals(3, tasks.size)
        assertEquals("First task", tasks[0].title)
        assertEquals("Second task", tasks[1].title)
        assertEquals("Third task", tasks[2].title)
    }

    @Test
    fun `parseVTodos with separate VCALENDAR blocks returns all tasks`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:task-a
            SUMMARY:Task A
            DTSTAMP:20231201T120000Z
            END:VTODO
            END:VCALENDAR
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:task-b
            SUMMARY:Task B
            DTSTAMP:20231201T120000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val tasks = parser.parseVTodos(icalData, accountId, listId, href, etag)

        assertEquals(2, tasks.size)
        assertEquals("Task A", tasks[0].title)
        assertEquals("Task B", tasks[1].title)
    }

    @Test
    fun `parseVTodos filters out tasks with missing UID`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:task-valid
            SUMMARY:Valid task
            DTSTAMP:20231201T120000Z
            END:VTODO
            BEGIN:VTODO
            SUMMARY:Invalid task without UID
            DTSTAMP:20231201T120000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val tasks = parser.parseVTodos(icalData, accountId, listId, href, etag)

        assertEquals(1, tasks.size)
        assertEquals("Valid task", tasks[0].title)
    }

    @Test
    fun `parseVTodos with malformed data returns empty list`() {
        val icalData = "Not valid iCalendar at all"

        val tasks = parser.parseVTodos(icalData, accountId, listId, href, etag)

        assertTrue(tasks.isEmpty())
    }

    @Test
    fun `parseVTodo with complex VTODO parses all fields`() {
        val icalData = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Nextcloud Tasks//EN
            BEGIN:VTODO
            UID:complex-task-123
            SUMMARY:Complex task
            DESCRIPTION:This is a detailed description\nwith multiple lines
            STATUS:IN-PROCESS
            PRIORITY:1
            DUE:20231225T235959Z
            LAST-MODIFIED:20231201T120000Z
            RELATED-TO:parent-task-789
            DTSTAMP:20231201T100000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val task = parser.parseVTodo(icalData, accountId, listId, href, etag)

        assertNotNull(task)
        assertEquals("complex-task-123", task.uid)
        assertEquals("Complex task", task.title)
        assertNotNull(task.description)
        assertEquals("IN-PROCESS", task.status)
        assertEquals(1, task.priority)
        assertNotNull(task.due)
        assertNotNull(task.updatedAt)
        assertEquals("parent-task-789", task.parentUid)
        assertEquals(false, task.completed) // IN-PROCESS is not COMPLETED
    }
}
