package com.nextcloud.tasks

import com.nextcloud.tasks.domain.model.Task
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskFavoriteTest {
    private val now = Instant.ofEpochMilli(1_700_000_000_000L)

    private fun task(
        priority: Int? = null,
        completed: Boolean = false,
        status: String? = null,
    ) = Task(
        id = "t",
        listId = "l",
        title = "t",
        updatedAt = now,
        uid = "t",
        priority = priority,
        completed = completed,
        status = status,
    )

    @Test
    fun `isStarred is true only for priority 1 to 4`() {
        assertFalse(task(priority = null).isStarred)
        assertFalse(task(priority = 0).isStarred)
        assertTrue(task(priority = 1).isStarred)
        assertTrue(task(priority = 4).isStarred)
        assertFalse(task(priority = 5).isStarred)
    }

    @Test
    fun `STARRED_PRIORITY renders as starred`() {
        assertEquals(1, STARRED_PRIORITY)
        assertTrue(task(priority = STARRED_PRIORITY).isStarred)
    }

    @Test
    fun `isEffectivelyDone covers completed and CANCELLED`() {
        assertFalse(task().isEffectivelyDone)
        assertTrue(task(completed = true).isEffectivelyDone)
        assertTrue(task(status = "CANCELLED").isEffectivelyDone)
        assertTrue(task(status = "cancelled").isEffectivelyDone)
        assertFalse(task(status = "NEEDS-ACTION").isEffectivelyDone)
    }
}
