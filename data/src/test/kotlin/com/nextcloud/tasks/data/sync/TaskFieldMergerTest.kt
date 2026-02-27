package com.nextcloud.tasks.data.sync

import com.nextcloud.tasks.data.database.entity.TaskEntity
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TaskFieldMergerTest {
    private val merger = TaskFieldMerger()

    private val now = Instant.ofEpochMilli(1700000000000L)
    private val later = Instant.ofEpochMilli(1700001000000L)

    @Suppress("LongParameterList")
    private fun createTask(
        id: String = "task-1",
        title: String = "Original Title",
        description: String? = "Original Description",
        completed: Boolean = false,
        due: Instant? = now,
        priority: Int? = 5,
        status: String? = "NEEDS-ACTION",
        completedAt: Instant? = null,
        parentUid: String? = null,
        baseSnapshot: String? = null,
    ) = TaskEntity(
        id = id,
        accountId = "account-1",
        listId = "list-1",
        title = title,
        description = description,
        completed = completed,
        due = due,
        updatedAt = now,
        priority = priority,
        status = status,
        completedAt = completedAt,
        uid = "uid-1",
        etag = "etag-1",
        href = "list-1/task-1.ics",
        parentUid = parentUid,
        baseSnapshot = baseSnapshot,
    )

    // --- createSnapshot / parseSnapshot round-trip ---

    @Test
    fun `createSnapshot produces valid JSON that round-trips`() {
        val task = createTask()
        val json = merger.createSnapshot(task)

        // JSON should contain the field values
        assert(json.contains("Original Title"))
        assert(json.contains("Original Description"))
    }

    @Test
    fun `createSnapshot handles null fields`() {
        val task =
            createTask(
                description = null,
                due = null,
                priority = null,
                status = null,
                completedAt = null,
                parentUid = null,
            )
        val json = merger.createSnapshot(task)
        assertNotNull(json)
    }

    // --- mergeTask without base snapshot ---

    @Test
    fun `mergeTask without base snapshot returns server task with snapshot`() {
        val serverTask = createTask(title = "Server Title")
        val localTask = createTask(title = "Local Title", baseSnapshot = null)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("Server Title", merged.title)
        assertNotNull(merged.baseSnapshot)
    }

    // --- mergeTask with base snapshot: no changes ---

    @Test
    fun `mergeTask with no changes preserves all fields`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask()
        val localTask = createTask(baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("Original Title", merged.title)
        assertEquals("Original Description", merged.description)
        assertEquals(false, merged.completed)
    }

    // --- mergeTask: only server changed ---

    @Test
    fun `mergeTask takes server value when only server changed title`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask(title = "Server Updated")
        val localTask = createTask(baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("Server Updated", merged.title)
    }

    @Test
    fun `mergeTask takes server value when only server changed description`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask(description = "Server Desc")
        val localTask = createTask(baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("Server Desc", merged.description)
    }

    // --- mergeTask: only local changed ---

    @Test
    fun `mergeTask preserves local title when only local changed`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask()
        val localTask = createTask(title = "Local Updated", baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("Local Updated", merged.title)
    }

    @Test
    fun `mergeTask preserves local description when only local changed`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask()
        val localTask = createTask(description = "Local Desc", baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("Local Desc", merged.description)
    }

    @Test
    fun `mergeTask preserves local completed when only local changed`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask()
        val localTask = createTask(completed = true, baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals(true, merged.completed)
    }

    @Test
    fun `mergeTask preserves local due when only local changed`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask()
        val localTask = createTask(due = later, baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals(later, merged.due)
    }

    @Test
    fun `mergeTask preserves local priority when only local changed`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask()
        val localTask = createTask(priority = 9, baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals(9, merged.priority)
    }

    @Test
    fun `mergeTask preserves local status when only local changed`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask()
        val localTask = createTask(status = "COMPLETED", baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("COMPLETED", merged.status)
    }

    @Test
    fun `mergeTask preserves local completedAt when only local changed`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask()
        val localTask = createTask(completedAt = later, baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals(later, merged.completedAt)
    }

    @Test
    fun `mergeTask preserves local parentUid when only local changed`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask()
        val localTask = createTask(parentUid = "parent-1", baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("parent-1", merged.parentUid)
    }

    // --- mergeTask: both changed same field ---

    @Test
    fun `mergeTask server wins when both changed title to different values`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask(title = "Server Title")
        val localTask = createTask(title = "Local Title", baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("Server Title", merged.title)
    }

    @Test
    fun `mergeTask uses shared value when both changed to same value`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask(title = "Same Title")
        val localTask = createTask(title = "Same Title", baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("Same Title", merged.title)
    }

    // --- mergeTask: mixed changes across fields ---

    @Test
    fun `mergeTask merges independent changes to different fields`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        // Server changed title, local changed description
        val serverTask = createTask(title = "Server Title")
        val localTask = createTask(description = "Local Desc", baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("Server Title", merged.title)
        assertEquals("Local Desc", merged.description)
    }

    @Test
    fun `mergeTask handles multiple field changes correctly`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        // Server: changed title + priority; Local: changed description + completed
        val serverTask = createTask(title = "Server Title", priority = 1)
        val localTask =
            createTask(
                description = "Local Desc",
                completed = true,
                baseSnapshot = snapshot,
            )

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("Server Title", merged.title)
        assertEquals("Local Desc", merged.description)
        assertEquals(true, merged.completed)
        assertEquals(1, merged.priority)
    }

    // --- mergeTask preserves server etag/href ---

    @Test
    fun `mergeTask uses server etag and href`() {
        val base = createTask()
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask().copy(etag = "new-etag", href = "new-href")
        val localTask = createTask(baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals("new-etag", merged.etag)
        assertEquals("new-href", merged.href)
    }

    // --- mergeTask: null value changes ---

    @Test
    fun `mergeTask preserves local change from non-null to null`() {
        val base = createTask(description = "Has desc")
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask(description = "Has desc")
        val localTask = createTask(description = null, baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertNull(merged.description)
    }

    @Test
    fun `mergeTask preserves local change from null to non-null`() {
        val base = createTask(completedAt = null)
        val snapshot = merger.createSnapshot(base)

        val serverTask = createTask(completedAt = null)
        val localTask = createTask(completedAt = later, baseSnapshot = snapshot)

        val merged = merger.mergeTask(serverTask, localTask)

        assertEquals(later, merged.completedAt)
    }

    // --- createSnapshot with due/completedAt epoch conversion ---

    @Test
    fun `createSnapshot converts due instant to epoch millis and back`() {
        val task = createTask(due = later)
        val snapshot = merger.createSnapshot(task)

        // Verify through round-trip: create a base with this due, then merge
        val serverTask = createTask(due = later)
        val localTask = createTask(due = later, baseSnapshot = snapshot)
        val merged = merger.mergeTask(serverTask, localTask)
        assertEquals(later, merged.due)
    }

    @Test
    fun `createSnapshot handles null due as null epoch`() {
        val task = createTask(due = null)
        val snapshot = merger.createSnapshot(task)
        assertNotNull(snapshot)
    }
}
