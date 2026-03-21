package com.nextcloud.tasks.data.repository

import androidx.room.withTransaction
import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.caldav.generator.VTodoGenerator
import com.nextcloud.tasks.data.caldav.parser.VTodoParser
import com.nextcloud.tasks.data.caldav.service.CalDavService
import com.nextcloud.tasks.data.database.NextcloudTasksDatabase
import com.nextcloud.tasks.data.database.dao.PendingOperationsDao
import com.nextcloud.tasks.data.database.dao.TagsDao
import com.nextcloud.tasks.data.database.dao.TaskListsDao
import com.nextcloud.tasks.data.database.dao.TasksDao
import com.nextcloud.tasks.data.database.entity.TaskListEntity
import com.nextcloud.tasks.data.mapper.TagMapper
import com.nextcloud.tasks.data.mapper.TaskListMapper
import com.nextcloud.tasks.data.mapper.TaskMapper
import com.nextcloud.tasks.data.network.NetworkMonitor
import com.nextcloud.tasks.data.sync.PendingOperationsManager
import com.nextcloud.tasks.data.sync.TaskFieldMerger
import com.nextcloud.tasks.domain.model.ShareAccess
import com.nextcloud.tasks.domain.model.TaskList
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTasksRepositoryListCrudTest {
    private val tasksDao = mockk<TasksDao>(relaxed = true)
    private val taskListsDao = mockk<TaskListsDao>(relaxed = true)
    private val tagsDao = mockk<TagsDao>(relaxed = true)
    private val pendingOperationsDao = mockk<PendingOperationsDao>(relaxed = true)
    private val database =
        mockk<NextcloudTasksDatabase>(relaxed = true) {
            every { tasksDao() } returns this@DefaultTasksRepositoryListCrudTest.tasksDao
            every { taskListsDao() } returns this@DefaultTasksRepositoryListCrudTest.taskListsDao
            every { tagsDao() } returns this@DefaultTasksRepositoryListCrudTest.tagsDao
            every { pendingOperationsDao() } returns this@DefaultTasksRepositoryListCrudTest.pendingOperationsDao
        }
    private val taskMapper = mockk<TaskMapper>()
    private val taskListMapper = mockk<TaskListMapper>()
    private val tagMapper = mockk<TagMapper>()
    private val calDavService = mockk<CalDavService>()
    private val vTodoParser = mockk<VTodoParser>()
    private val vTodoGenerator = mockk<VTodoGenerator>()
    private val networkMonitor = mockk<NetworkMonitor>()
    private val pendingOperationsManager = mockk<PendingOperationsManager>(relaxed = true)
    private val taskFieldMerger = mockk<TaskFieldMerger>(relaxed = true)
    private val authTokenProvider =
        mockk<AuthTokenProvider> {
            coEvery { activeAccountId() } returns "account-1"
            coEvery { activeServerUrl() } returns "https://cloud.example.com"
            every { observeActiveAccountId() } returns flowOf("account-1")
        }

    private val testDispatcher = UnconfinedTestDispatcher()

    private fun createRepository(): DefaultTasksRepository {
        mockkStatic("androidx.room.RoomDatabaseKt")
        val transactionBlock = slot<suspend () -> Unit>()
        coEvery { database.withTransaction(capture(transactionBlock)) } coAnswers {
            transactionBlock.captured.invoke()
        }
        return DefaultTasksRepository(
            database = database,
            taskMapper = taskMapper,
            taskListMapper = taskListMapper,
            tagMapper = tagMapper,
            calDavService = calDavService,
            vTodoParser = vTodoParser,
            vTodoGenerator = vTodoGenerator,
            authTokenProvider = authTokenProvider,
            networkMonitor = networkMonitor,
            pendingOperationsManager = pendingOperationsManager,
            taskFieldMerger = taskFieldMerger,
            ioDispatcher = testDispatcher,
        )
    }

    private fun taskListEntity(
        id: String = "/dav/lists/my-list/",
        name: String = "My List",
        color: String? = "#0082C9",
        href: String = "/dav/lists/my-list/",
    ) = TaskListEntity(
        id = id,
        accountId = "account-1",
        name = name,
        color = color,
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        etag = "\"etag-1\"",
        href = href,
        order = null,
        shareAccess = "OWNER",
        isShared = false,
    )

    private fun domainTaskList(entity: TaskListEntity) =
        TaskList(
            id = entity.id,
            name = entity.name,
            color = entity.color,
            updatedAt = entity.updatedAt,
            etag = entity.etag,
            href = entity.href,
            shareAccess = ShareAccess.OWNER,
        )

    // ── updateTaskList ────────────────────────────────────────────────────────

    @Test
    fun `updateTaskList sends PROPPATCH and persists updated entity`() =
        runTest(testDispatcher) {
            val entity = taskListEntity()
            val updatedEntity = entity.copy(name = "Renamed", color = "#46BA61")
            val expectedDomain = domainTaskList(updatedEntity)

            coEvery { taskListsDao.getTaskList(entity.id) } returns entity
            coEvery { calDavService.updateCalendarProperties(any(), any(), any(), any()) } returns Result.success(Unit)
            every { taskListMapper.toDomain(any()) } returns expectedDomain

            val repo = createRepository()
            val result = repo.updateTaskList(entity.id, "Renamed", "#46BA61")

            coVerify {
                calDavService.updateCalendarProperties(
                    "https://cloud.example.com",
                    entity.href!!,
                    "Renamed",
                    "#46BA61",
                )
            }
            coVerify { taskListsDao.upsertTaskList(match { it.name == "Renamed" && it.color == "#46BA61" }) }
            assertEquals(expectedDomain, result)
        }

    @Test
    fun `updateTaskList preserves null color when no color provided`() =
        runTest(testDispatcher) {
            val entity = taskListEntity(color = null)
            val updatedEntity = entity.copy(name = "No Color List", color = null)
            val expectedDomain = domainTaskList(updatedEntity)

            coEvery { taskListsDao.getTaskList(entity.id) } returns entity
            coEvery { calDavService.updateCalendarProperties(any(), any(), any(), null) } returns Result.success(Unit)
            every { taskListMapper.toDomain(any()) } returns expectedDomain

            val repo = createRepository()
            repo.updateTaskList(entity.id, "No Color List", null)

            coVerify { calDavService.updateCalendarProperties(any(), any(), "No Color List", null) }
            coVerify { taskListsDao.upsertTaskList(match { it.color == null }) }
        }

    @Test
    fun `updateTaskList throws when list not found in DB`() =
        runTest(testDispatcher) {
            coEvery { taskListsDao.getTaskList(any()) } returns null

            val repo = createRepository()
            assertFailsWith<IOException> {
                repo.updateTaskList("unknown-id", "Name", null)
            }

            coVerify(exactly = 0) { calDavService.updateCalendarProperties(any(), any(), any(), any()) }
        }

    @Test
    fun `updateTaskList throws when CalDAV call fails`() =
        runTest(testDispatcher) {
            val entity = taskListEntity()
            coEvery { taskListsDao.getTaskList(entity.id) } returns entity
            coEvery {
                calDavService.updateCalendarProperties(any(), any(), any(), any())
            } returns Result.failure(IOException("Server error"))

            val repo = createRepository()
            assertFailsWith<IOException> {
                repo.updateTaskList(entity.id, "New Name", "#E9322D")
            }

            coVerify(exactly = 0) { taskListsDao.upsertTaskList(any()) }
        }

    // ── deleteTaskList ────────────────────────────────────────────────────────

    @Test
    fun `deleteTaskList removes all related rows in correct order`() =
        runTest(testDispatcher) {
            val entity = taskListEntity()
            coEvery { taskListsDao.getTaskList(entity.id) } returns entity
            coEvery { calDavService.deleteCalendarCollection(any(), any()) } returns Result.success(Unit)

            val repo = createRepository()
            repo.deleteTaskList(entity.id)

            coVerify { calDavService.deleteCalendarCollection("https://cloud.example.com", entity.href!!) }
            coVerify { pendingOperationsDao.deleteByListId(entity.id) }
            coVerify { tasksDao.deleteTagsByListId(entity.id) }
            coVerify { tasksDao.deleteTasksByListId(entity.id) }
            coVerify { taskListsDao.deleteTaskList(entity.id) }
        }

    @Test
    fun `deleteTaskList wraps DB cleanup in a transaction`() =
        runTest(testDispatcher) {
            val entity = taskListEntity()
            coEvery { taskListsDao.getTaskList(entity.id) } returns entity
            coEvery { calDavService.deleteCalendarCollection(any(), any()) } returns Result.success(Unit)

            val repo = createRepository()
            repo.deleteTaskList(entity.id)

            coVerify { database.withTransaction(any()) }
        }

    @Test
    fun `deleteTaskList throws when CalDAV call fails and does not touch DB`() =
        runTest(testDispatcher) {
            val entity = taskListEntity()
            coEvery { taskListsDao.getTaskList(entity.id) } returns entity
            coEvery {
                calDavService.deleteCalendarCollection(any(), any())
            } returns Result.failure(IOException("Network error"))

            val repo = createRepository()
            assertFailsWith<IOException> {
                repo.deleteTaskList(entity.id)
            }

            coVerify(exactly = 0) { tasksDao.deleteTasksByListId(any()) }
            coVerify(exactly = 0) { taskListsDao.deleteTaskList(any()) }
        }

    @Test
    fun `deleteTaskList throws when list not found in DB`() =
        runTest(testDispatcher) {
            coEvery { taskListsDao.getTaskList(any()) } returns null

            val repo = createRepository()
            assertFailsWith<IOException> {
                repo.deleteTaskList("unknown-id")
            }

            coVerify(exactly = 0) { calDavService.deleteCalendarCollection(any(), any()) }
        }
}
