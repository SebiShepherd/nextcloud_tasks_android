package com.nextcloud.tasks

import com.nextcloud.tasks.domain.model.Task
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubtaskTreeTest {
    private val now = Instant.ofEpochMilli(1_700_000_000_000L)

    private fun task(
        uid: String,
        parentUid: String? = null,
    ) = Task(id = uid, listId = "l1", title = uid, updatedAt = now, uid = uid, parentUid = parentUid)

    @Test
    fun `flat list keeps order and depth 0`() {
        val tasks = listOf(task("a"), task("b"), task("c"))
        val rows = buildOpenTaskRows(tasks, emptyMap(), emptySet())
        assertEquals(listOf("a", "b", "c"), rows.map { it.task.uid })
        assertTrue(rows.all { it.depth == 0 && !it.hasChildren })
    }

    @Test
    fun `children nest under parent at depth 1`() {
        val tasks = listOf(task("p"), task("c1", parentUid = "p"), task("c2", parentUid = "p"))
        val rows = buildOpenTaskRows(tasks, mapOf("p" to (0 to 2)), emptySet())
        assertEquals(listOf("p", "c1", "c2"), rows.map { it.task.uid })
        assertEquals(listOf(0, 1, 1), rows.map { it.depth })
        assertTrue(rows.first().hasChildren)
        assertEquals(2, rows.first().subtaskTotal)
    }

    @Test
    fun `depth is capped at MAX_DISPLAY_DEPTH for deep nesting`() {
        // a>b>c>d>e>f: natural depths 0..5, capped at MAX_DISPLAY_DEPTH (4).
        val chain = listOf("a", "b", "c", "d", "e", "f")
        val tasks = chain.mapIndexed { i, uid -> task(uid, parentUid = chain.getOrNull(i - 1)) }
        val rows = buildOpenTaskRows(tasks, emptyMap(), emptySet())
        assertEquals(listOf(0, 1, 2, 3, 4, 4), rows.map { it.depth })
    }

    @Test
    fun `collapsed parent hides its descendants`() {
        val tasks = listOf(task("p"), task("c", parentUid = "p"))
        val rows = buildOpenTaskRows(tasks, mapOf("p" to (0 to 1)), collapsedUids = setOf("p"))
        assertEquals(listOf("p"), rows.map { it.task.uid })
        assertTrue(rows.single().isCollapsed)
    }

    @Test
    fun `done child stays nested under an open parent`() {
        val tasks = listOf(task("p"), task("c", parentUid = "p").copy(completed = true))
        val rows = buildOpenTaskRows(tasks, mapOf("p" to (1 to 1)), emptySet())
        assertEquals(listOf("p", "c"), rows.map { it.task.uid })
        assertEquals(listOf(0, 1), rows.map { it.depth })
    }

    @Test
    fun `done root is not surfaced in the open tree`() {
        val tasks = listOf(task("p").copy(completed = true), task("s", parentUid = "p"))
        val rows = buildOpenTaskRows(tasks, emptyMap(), emptySet())
        assertTrue(rows.none { it.task.uid == "p" })
    }

    @Test
    fun `cycle does not loop forever and emits each task once`() {
        val tasks = listOf(task("a", parentUid = "b"), task("b", parentUid = "a"))
        val rows = buildOpenTaskRows(tasks, emptyMap(), emptySet())
        assertEquals(2, rows.size)
        assertEquals(setOf("a", "b"), rows.map { it.task.uid }.toSet())
    }

    @Test
    fun `completed rows nest a done child under its done parent`() {
        val tasks =
            listOf(
                task("p").copy(completed = true),
                task("c", parentUid = "p").copy(completed = true),
            )
        val rows = buildCompletedTaskRows(tasks, emptySet())
        assertEquals(listOf("p", "c"), rows.map { it.task.uid })
        assertEquals(listOf(0, 1), rows.map { it.depth })
        assertTrue(rows.first().hasChildren)
    }

    @Test
    fun `orphan child whose parent is absent shows at top level`() {
        val tasks = listOf(task("c", parentUid = "missing"))
        val rows = buildOpenTaskRows(tasks, emptyMap(), emptySet())
        assertEquals(1, rows.size)
        assertEquals(0, rows.single().depth)
    }
}
