package com.nextcloud.tasks.data.repository

import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.caldav.generator.VTodoGenerator
import com.nextcloud.tasks.data.caldav.parser.VTodoParser
import com.nextcloud.tasks.data.caldav.service.CalDavService
import com.nextcloud.tasks.data.database.NextcloudTasksDatabase
import com.nextcloud.tasks.data.database.dao.PendingOperationsDao
import com.nextcloud.tasks.data.database.dao.TagsDao
import com.nextcloud.tasks.data.database.dao.TaskListsDao
import com.nextcloud.tasks.data.database.dao.TasksDao
import com.nextcloud.tasks.data.database.entity.TaskEntity
import com.nextcloud.tasks.data.database.model.TaskWithRelations
import com.nextcloud.tasks.data.mapper.TagMapper
import com.nextcloud.tasks.data.mapper.TaskListMapper
import com.nextcloud.tasks.data.mapper.TaskMapper
import com.nextcloud.tasks.data.network.NetworkMonitor
import com.nextcloud.tasks.data.sync.PendingOperationsManager
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskDraft
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
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTasksRepositoryOfflineTest {
    private val tasksDao = mockk<TasksDao>(relaxed = true)
    private val taskListsDao = mockk<TaskListsDao>(relaxed = true)
    private val tagsDao = mockk<TagsDao>(relaxed = true)
    private val pendingOperationsDao = mockk<PendingOperationsDao>(relaxed = true)
    private val database =
        mockk<NextcloudTasksDatabase>(relaxed = true) {
            every { tasksDao() } returns this@DefaultTasksRepositoryOfflineTest.tasksDao
            every { taskListsDao() } returns this@DefaultTasksRepositoryOfflineTest.taskListsDao
            every { tagsDao() } returns this@DefaultTasksRepositoryOfflineTest.tagsDao
            every { pendingOperationsDao() } returns this@DefaultTasksRepositoryOfflineTest.pendingOperationsDao
        }
    private val taskMapper = mockk<TaskMapper>()
    private val taskListMapper = mockk<TaskListMapper>()
    private val tagMapper = mockk<TagMapper>()
    private val calDavService = mockk<CalDavService>()
    private val vTodoParser = mockk<VTodoParser>()
    private val vTodoGenerator = mockk<VTodoGenerator>()
    private val networkMonitor = mockk<NetworkMonitor>()
    private val pendingOperationsManager = mockk<PendingOperationsManager>(relaxed = true)
    private val authTokenProvider =
        mockk<AuthTokenProvider> {
            coEvery { activeAccountId() } returns "account-1"
            coEvery { activeServerUrl() } returns "https://cloud.example.com"
            every { observeActiveAccountId() } returns flowOf("account-1")
        }

    private val testDispatcher = UnconfinedTestDispatcher()

    private fun createRepository(): DefaultTasksRepository {
        // Mock withTransaction to just execute the block
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
            ioDispatcher = testDispatcher,
        )
    }

    private fun createDomainTask(
        id: String = "task-1",
        completed: Boolean = false,
        href: String? = "list-1/$id.ics",
    ) = Task(
        id = id,
        listId = "list-1",
        title = "Test Task",
        description = "Description",
        completed = completed,
        due = null,
        updatedAt = Instant.ofEpochMilli(1700000000000L),
        tags = emptyList(),
        priority = null,
        status = if (completed) "COMPLETED" else "NEEDS-ACTION",
        completedAt = if (completed) Instant.ofEpochMilli(1700000000000L) else null,
        uid = "uid-$id",
        etag = "etag-1",
        href = href,
        parentUid = null,
    )

    // --- createTask Tests ---

    @Test
    fun `createTask saves locally when offline and queues operation`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            every { networkMonitor.isCurrentlyOnline() } returns false

            val taskWithRelations = mockk<TaskWithRelations>()
            val domainTask = createDomainTask(href = null)
            every { taskMapper.toDomain(taskWithRelations) } returns domainTask
            coEvery { tasksDao.getTaskWithRelations(any()) } returns taskWithRelations

            val draft =
                TaskDraft(
                    listId = "list-1",
                    title = "Test Task",
                    description = "Description",
                    completed = false,
                    due = null,
                )

            repo.createTask(draft)

            // Should save to local database
            coVerify { tasksDao.upsertTask(match { it.title == "Test Task" && it.href == null }) }
            // Should queue for later sync
            coVerify { pendingOperationsManager.queueCreateOperation(any(), "list-1") }
            // Should NOT try to sync to server
            coVerify(exactly = 0) { calDavService.createTodo(any(), any(), any(), any()) }
        }

    @Test
    fun `createTask saves locally and syncs when online`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            every { networkMonitor.isCurrentlyOnline() } returns true

            val taskWithRelations = mockk<TaskWithRelations>()
            val domainTask = createDomainTask(href = null)
            every { taskMapper.toDomain(taskWithRelations) } returns domainTask
            coEvery { tasksDao.getTaskWithRelations(any()) } returns taskWithRelations

            val draft =
                TaskDraft(
                    listId = "list-1",
                    title = "Test Task",
                    description = null,
                    completed = false,
                    due = null,
                )

            repo.createTask(draft)

            // Should save to local database immediately
            coVerify { tasksDao.upsertTask(match { it.title == "Test Task" && it.href == null }) }
            // Should NOT queue (online = sync in background)
            coVerify(exactly = 0) { pendingOperationsManager.queueCreateOperation(any(), any()) }
        }

    // --- updateTask Tests ---

    @Test
    fun `updateTask saves locally when offline and queues operation`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            every { networkMonitor.isCurrentlyOnline() } returns false

            val task = createDomainTask(completed = true)
            val taskWithRelations = mockk<TaskWithRelations>()
            every { taskMapper.toDomain(taskWithRelations) } returns task
            coEvery { tasksDao.getTaskWithRelations("task-1") } returns taskWithRelations

            repo.updateTask(task)

            // Should save to local database immediately
            coVerify { tasksDao.upsertTask(match { it.id == "task-1" && it.completed }) }
            // Should queue for later sync
            coVerify { pendingOperationsManager.queueUpdateOperation(any()) }
            // Should NOT try to sync to server
            coVerify(exactly = 0) { calDavService.updateTodo(any(), any(), any(), any()) }
        }

    @Test
    fun `updateTask saves locally and syncs when online`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            every { networkMonitor.isCurrentlyOnline() } returns true

            val task = createDomainTask(completed = true)
            val taskWithRelations = mockk<TaskWithRelations>()
            every { taskMapper.toDomain(taskWithRelations) } returns task
            coEvery { tasksDao.getTaskWithRelations("task-1") } returns taskWithRelations

            repo.updateTask(task)

            // Should save to local database immediately (optimistic)
            coVerify { tasksDao.upsertTask(match { it.id == "task-1" && it.completed }) }
            // Should NOT queue (online = sync in background)
            coVerify(exactly = 0) { pendingOperationsManager.queueUpdateOperation(any()) }
        }

    @Test
    fun `updateTask does not queue without href`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            every { networkMonitor.isCurrentlyOnline() } returns false

            val task = createDomainTask(href = null)
            val taskWithRelations = mockk<TaskWithRelations>()
            every { taskMapper.toDomain(taskWithRelations) } returns task
            coEvery { tasksDao.getTaskWithRelations("task-1") } returns taskWithRelations

            repo.updateTask(task)

            // Should save locally
            coVerify { tasksDao.upsertTask(any()) }
            // Should NOT queue (no href means no server resource yet)
            coVerify(exactly = 0) { pendingOperationsManager.queueUpdateOperation(any()) }
        }

    // --- deleteTask Tests ---

    @Test
    fun `deleteTask deletes locally when offline and queues operation`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            every { networkMonitor.isCurrentlyOnline() } returns false

            val task = createDomainTask()
            val taskWithRelations = mockk<TaskWithRelations>()
            every { taskMapper.toDomain(taskWithRelations) } returns task
            coEvery { tasksDao.getTaskWithRelations("task-1") } returns taskWithRelations

            repo.deleteTask("task-1")

            // Should delete from local database immediately
            coVerify { tasksDao.deleteTask("task-1") }
            coVerify { tasksDao.clearTagsForTask("task-1") }
            // Should queue for later sync
            coVerify {
                pendingOperationsManager.queueDeleteOperation(
                    "task-1",
                    "list-1/task-1.ics",
                    "etag-1",
                )
            }
            // Should NOT try to sync to server
            coVerify(exactly = 0) { calDavService.deleteTodo(any(), any(), any()) }
        }

    @Test
    fun `deleteTask deletes locally and syncs when online`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            every { networkMonitor.isCurrentlyOnline() } returns true

            val task = createDomainTask()
            val taskWithRelations = mockk<TaskWithRelations>()
            every { taskMapper.toDomain(taskWithRelations) } returns task
            coEvery { tasksDao.getTaskWithRelations("task-1") } returns taskWithRelations

            repo.deleteTask("task-1")

            // Should delete from local database immediately
            coVerify { tasksDao.deleteTask("task-1") }
            // Should NOT queue (online = sync in background)
            coVerify(exactly = 0) { pendingOperationsManager.queueDeleteOperation(any(), any(), any()) }
        }

    @Test
    fun `deleteTask does not queue without href`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            every { networkMonitor.isCurrentlyOnline() } returns false

            val task = createDomainTask(href = null)
            val taskWithRelations = mockk<TaskWithRelations>()
            every { taskMapper.toDomain(taskWithRelations) } returns task
            coEvery { tasksDao.getTaskWithRelations("task-1") } returns taskWithRelations

            repo.deleteTask("task-1")

            // Should delete locally
            coVerify { tasksDao.deleteTask("task-1") }
            // Should NOT queue (no href = no server resource)
            coVerify(exactly = 0) { pendingOperationsManager.queueDeleteOperation(any(), any(), any()) }
        }

    // --- refresh Tests ---

    @Test
    fun `refresh processes pending operations before fetching from server`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            every { networkMonitor.isCurrentlyOnline() } returns true
            coEvery { pendingOperationsManager.getTaskIdsWithPendingCreate() } returns emptyList()
            coEvery { calDavService.discoverPrincipal(any()) } returns
                Result.failure(Exception("Not relevant for this test"))

            repo.refresh()

            // Should process pending operations first
            coVerify(exactly = 1) { pendingOperationsManager.processPendingOperations() }
        }

    @Test
    fun `refresh skips pending operations when offline`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            every { networkMonitor.isCurrentlyOnline() } returns false
            coEvery { authTokenProvider.activeServerUrl() } returns null

            repo.refresh()

            coVerify(exactly = 0) { pendingOperationsManager.processPendingOperations() }
        }
}
