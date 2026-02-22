package com.nextcloud.tasks.data.sync

import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.caldav.generator.VTodoGenerator
import com.nextcloud.tasks.data.caldav.service.CalDavService
import com.nextcloud.tasks.data.database.NextcloudTasksDatabase
import com.nextcloud.tasks.data.database.dao.PendingOperationsDao
import com.nextcloud.tasks.data.database.dao.TasksDao
import com.nextcloud.tasks.data.database.entity.PendingOperationEntity
import com.nextcloud.tasks.data.database.entity.TaskEntity
import com.nextcloud.tasks.data.database.model.TaskWithRelations
import com.nextcloud.tasks.data.network.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PendingOperationsManagerTest {
    private val pendingOperationsDao = mockk<PendingOperationsDao>(relaxed = true)
    private val tasksDao = mockk<TasksDao>(relaxed = true)
    private val database =
        mockk<NextcloudTasksDatabase> {
            every { pendingOperationsDao() } returns this@PendingOperationsManagerTest.pendingOperationsDao
            every { tasksDao() } returns this@PendingOperationsManagerTest.tasksDao
        }
    private val networkMonitor =
        mockk<NetworkMonitor> {
            // Emit false to avoid auto-triggering processPendingOperations in init
            every { isOnline } returns flowOf(false)
            every { isCurrentlyOnline() } returns false
        }
    private val calDavService = mockk<CalDavService>()
    private val vTodoGenerator = mockk<VTodoGenerator>()
    private val authTokenProvider =
        mockk<AuthTokenProvider> {
            coEvery { activeAccountId() } returns "account-1"
            coEvery { activeServerUrl() } returns "https://cloud.example.com"
            every { observeActiveAccountId() } returns flowOf("account-1")
        }

    private val testDispatcher = UnconfinedTestDispatcher()

    private fun createManager() =
        PendingOperationsManager(
            database = database,
            networkMonitor = networkMonitor,
            calDavService = calDavService,
            vTodoGenerator = vTodoGenerator,
            authTokenProvider = authTokenProvider,
            ioDispatcher = testDispatcher,
        )

    private fun createTaskEntity(
        id: String = "task-1",
        completed: Boolean = false,
    ) = TaskEntity(
        id = id,
        accountId = "account-1",
        listId = "list-1",
        title = "Test Task",
        description = "Description",
        completed = completed,
        due = null,
        updatedAt = Instant.ofEpochMilli(1700000000000L),
        priority = null,
        status = if (completed) "COMPLETED" else "NEEDS-ACTION",
        completedAt = if (completed) Instant.ofEpochMilli(1700000000000L) else null,
        uid = "uid-$id",
        etag = "etag-1",
        href = "list-1/$id.ics",
        parentUid = null,
    )

    // --- Queue Operations Tests ---

    @Test
    fun `queueUpdateOperation inserts pending operation with correct type`() =
        runTest(testDispatcher) {
            val manager = createManager()
            val task = createTaskEntity()
            val slot = slot<PendingOperationEntity>()

            coEvery { pendingOperationsDao.insert(capture(slot)) } returns 1L

            manager.queueUpdateOperation(task)

            coVerify { pendingOperationsDao.deleteByTaskId("task-1") }
            assertTrue(slot.isCaptured)
            assertEquals(PendingOperationEntity.OPERATION_UPDATE, slot.captured.operationType)
            assertEquals("account-1", slot.captured.accountId)
            assertEquals("task-1", slot.captured.taskId)
        }

    @Test
    fun `queueDeleteOperation inserts pending operation with correct type`() =
        runTest(testDispatcher) {
            val manager = createManager()
            val slot = slot<PendingOperationEntity>()

            coEvery { pendingOperationsDao.insert(capture(slot)) } returns 1L

            manager.queueDeleteOperation("task-1", "list-1/task-1.ics", "etag-1")

            coVerify { pendingOperationsDao.deleteByTaskId("task-1") }
            assertTrue(slot.isCaptured)
            assertEquals(PendingOperationEntity.OPERATION_DELETE, slot.captured.operationType)
            assertEquals("task-1", slot.captured.taskId)
        }

    @Test
    fun `queueCreateOperation inserts pending operation with correct type`() =
        runTest(testDispatcher) {
            val manager = createManager()
            val task = createTaskEntity()
            val slot = slot<PendingOperationEntity>()

            coEvery { pendingOperationsDao.insert(capture(slot)) } returns 1L

            manager.queueCreateOperation(task, "list-1")

            coVerify { pendingOperationsDao.deleteByTaskId("task-1") }
            assertTrue(slot.isCaptured)
            assertEquals(PendingOperationEntity.OPERATION_CREATE, slot.captured.operationType)
            assertEquals("task-1", slot.captured.taskId)
        }

    @Test
    fun `queueUpdateOperation does nothing without active account`() =
        runTest(testDispatcher) {
            coEvery { authTokenProvider.activeAccountId() } returns null
            val manager = createManager()

            manager.queueUpdateOperation(createTaskEntity())

            coVerify(exactly = 0) { pendingOperationsDao.insert(any()) }
        }

    @Test
    fun `queueUpdateOperation replaces existing operation for same task`() =
        runTest(testDispatcher) {
            val manager = createManager()
            val task = createTaskEntity()

            coEvery { pendingOperationsDao.insert(any()) } returns 1L

            manager.queueUpdateOperation(task)

            // Should delete existing operations before inserting new one
            coVerify(exactly = 1) { pendingOperationsDao.deleteByTaskId("task-1") }
            coVerify(exactly = 1) { pendingOperationsDao.insert(any()) }
        }

    // --- Process Operations Tests ---

    @Test
    fun `processPendingOperations processes update operation successfully`() =
        runTest(testDispatcher) {
            val manager = createManager()
            val task = createTaskEntity()
            val taskWithRelations =
                mockk<TaskWithRelations> {
                    every { this@mockk.task } returns task
                }

            val operation =
                PendingOperationEntity(
                    id = 1,
                    accountId = "account-1",
                    taskId = "task-1",
                    operationType = PendingOperationEntity.OPERATION_UPDATE,
                    payload = createUpdatePayloadJson(task),
                    createdAt = Instant.now(),
                )

            coEvery { pendingOperationsDao.getPendingOperations("account-1") } returns listOf(operation)
            coEvery { tasksDao.getTaskWithRelations("task-1") } returns taskWithRelations
            every { vTodoGenerator.generateVTodo(any()) } returns "BEGIN:VCALENDAR..."
            coEvery { calDavService.updateTodo(any(), any(), any(), any()) } returns Result.success("new-etag")

            manager.processPendingOperations()

            coVerify { calDavService.updateTodo(any(), "list-1/task-1.ics", any(), any()) }
            coVerify { tasksDao.upsertTask(match { it.etag == "new-etag" }) }
            coVerify { pendingOperationsDao.delete(1) }
        }

    @Test
    fun `processPendingOperations processes delete operation successfully`() =
        runTest(testDispatcher) {
            val manager = createManager()

            val operation =
                PendingOperationEntity(
                    id = 2,
                    accountId = "account-1",
                    taskId = "task-1",
                    operationType = PendingOperationEntity.OPERATION_DELETE,
                    payload = createDeletePayloadJson("task-1", "list-1/task-1.ics", "etag-1"),
                    createdAt = Instant.now(),
                )

            coEvery { pendingOperationsDao.getPendingOperations("account-1") } returns listOf(operation)
            coEvery { calDavService.deleteTodo(any(), any(), any()) } returns Result.success(Unit)

            manager.processPendingOperations()

            coVerify { calDavService.deleteTodo(any(), "list-1/task-1.ics", "etag-1") }
            coVerify { pendingOperationsDao.delete(2) }
        }

    @Test
    fun `processPendingOperations processes create operation successfully`() =
        runTest(testDispatcher) {
            val manager = createManager()

            val operation =
                PendingOperationEntity(
                    id = 3,
                    accountId = "account-1",
                    taskId = "task-1",
                    operationType = PendingOperationEntity.OPERATION_CREATE,
                    payload = createCreatePayloadJson("task-1", "uid-task-1"),
                    createdAt = Instant.now(),
                )

            coEvery { pendingOperationsDao.getPendingOperations("account-1") } returns listOf(operation)
            every { vTodoGenerator.generateVTodo(any()) } returns "BEGIN:VCALENDAR..."
            every { vTodoGenerator.generateFilename(any()) } returns "uid-task-1.ics"
            coEvery { calDavService.createTodo(any(), any(), any(), any()) } returns Result.success("new-etag")

            manager.processPendingOperations()

            coVerify { calDavService.createTodo(any(), "list-1", "uid-task-1.ics", any()) }
            coVerify { tasksDao.upsertTask(match { it.etag == "new-etag" && it.href == "list-1/uid-task-1.ics" }) }
            coVerify { pendingOperationsDao.delete(3) }
        }

    // --- Error Handling and Retry Tests ---

    @Test
    fun `processPendingOperations increments retry on failure`() =
        runTest(testDispatcher) {
            val manager = createManager()
            val task = createTaskEntity()
            val taskWithRelations =
                mockk<TaskWithRelations> {
                    every { this@mockk.task } returns task
                }

            val operation =
                PendingOperationEntity(
                    id = 1,
                    accountId = "account-1",
                    taskId = "task-1",
                    operationType = PendingOperationEntity.OPERATION_UPDATE,
                    payload = createUpdatePayloadJson(task),
                    createdAt = Instant.now(),
                    retryCount = 0,
                )

            coEvery { pendingOperationsDao.getPendingOperations("account-1") } returns listOf(operation)
            coEvery { tasksDao.getTaskWithRelations("task-1") } returns taskWithRelations
            every { vTodoGenerator.generateVTodo(any()) } returns "BEGIN:VCALENDAR..."
            coEvery { calDavService.updateTodo(any(), any(), any(), any()) } returns
                Result.failure(IOException("Network error"))

            manager.processPendingOperations()

            coVerify { pendingOperationsDao.incrementRetryCount(1, "Network error") }
            coVerify(exactly = 0) { pendingOperationsDao.delete(1) }
        }

    @Test
    fun `processPendingOperations removes operation after max retries`() =
        runTest(testDispatcher) {
            val manager = createManager()
            val task = createTaskEntity()
            val taskWithRelations =
                mockk<TaskWithRelations> {
                    every { this@mockk.task } returns task
                }

            val operation =
                PendingOperationEntity(
                    id = 1,
                    accountId = "account-1",
                    taskId = "task-1",
                    operationType = PendingOperationEntity.OPERATION_UPDATE,
                    payload = createUpdatePayloadJson(task),
                    createdAt = Instant.now(),
                    retryCount = 5, // MAX_RETRIES
                )

            coEvery { pendingOperationsDao.getPendingOperations("account-1") } returns listOf(operation)
            coEvery { tasksDao.getTaskWithRelations("task-1") } returns taskWithRelations
            every { vTodoGenerator.generateVTodo(any()) } returns "BEGIN:VCALENDAR..."
            coEvery { calDavService.updateTodo(any(), any(), any(), any()) } returns
                Result.failure(IOException("Network error"))

            manager.processPendingOperations()

            coVerify { pendingOperationsDao.incrementRetryCount(1, "Network error") }
            coVerify { pendingOperationsDao.delete(1) }
        }

    @Test
    fun `processPendingOperations catches non-IOException exceptions`() =
        runTest(testDispatcher) {
            val manager = createManager()
            val task = createTaskEntity()
            val taskWithRelations =
                mockk<TaskWithRelations> {
                    every { this@mockk.task } returns task
                }

            val operation =
                PendingOperationEntity(
                    id = 1,
                    accountId = "account-1",
                    taskId = "task-1",
                    operationType = PendingOperationEntity.OPERATION_UPDATE,
                    payload = createUpdatePayloadJson(task),
                    createdAt = Instant.now(),
                    retryCount = 0,
                )

            coEvery { pendingOperationsDao.getPendingOperations("account-1") } returns listOf(operation)
            coEvery { tasksDao.getTaskWithRelations("task-1") } returns taskWithRelations
            every { vTodoGenerator.generateVTodo(any()) } returns "BEGIN:VCALENDAR..."
            coEvery { calDavService.updateTodo(any(), any(), any(), any()) } throws
                RuntimeException("JSON parse error")

            // Should NOT throw - the exception should be caught
            manager.processPendingOperations()

            coVerify { pendingOperationsDao.incrementRetryCount(1, "JSON parse error") }
        }

    @Test
    fun `processPendingOperations continues after one operation fails`() =
        runTest(testDispatcher) {
            val manager = createManager()

            val op1 =
                PendingOperationEntity(
                    id = 1,
                    accountId = "account-1",
                    taskId = "task-1",
                    operationType = PendingOperationEntity.OPERATION_DELETE,
                    payload = createDeletePayloadJson("task-1", "list-1/task-1.ics", "etag-1"),
                    createdAt = Instant.now(),
                )
            val op2 =
                PendingOperationEntity(
                    id = 2,
                    accountId = "account-1",
                    taskId = "task-2",
                    operationType = PendingOperationEntity.OPERATION_DELETE,
                    payload = createDeletePayloadJson("task-2", "list-1/task-2.ics", "etag-2"),
                    createdAt = Instant.now(),
                )

            coEvery { pendingOperationsDao.getPendingOperations("account-1") } returns listOf(op1, op2)
            // First delete fails, second succeeds
            coEvery { calDavService.deleteTodo(any(), "list-1/task-1.ics", any()) } returns
                Result.failure(IOException("Network error"))
            coEvery { calDavService.deleteTodo(any(), "list-1/task-2.ics", any()) } returns
                Result.success(Unit)

            manager.processPendingOperations()

            // First op should increment retry, second should be deleted (success)
            coVerify { pendingOperationsDao.incrementRetryCount(1, any()) }
            coVerify { pendingOperationsDao.delete(2) }
        }

    @Test
    fun `processPendingOperations skips without active account`() =
        runTest(testDispatcher) {
            coEvery { authTokenProvider.activeAccountId() } returns null
            val manager = createManager()

            manager.processPendingOperations()

            coVerify(exactly = 0) { pendingOperationsDao.getPendingOperations(any()) }
        }

    // --- getTaskIdsWithPendingCreate Tests ---

    @Test
    fun `getTaskIdsWithPendingCreate returns task IDs from DAO`() =
        runTest(testDispatcher) {
            val manager = createManager()
            coEvery { pendingOperationsDao.getTaskIdsWithPendingCreate("account-1") } returns
                listOf("task-1", "task-2")

            val result = manager.getTaskIdsWithPendingCreate()

            assertEquals(listOf("task-1", "task-2"), result)
        }

    @Test
    fun `getTaskIdsWithPendingCreate returns empty without active account`() =
        runTest(testDispatcher) {
            coEvery { authTokenProvider.activeAccountId() } returns null
            val manager = createManager()

            val result = manager.getTaskIdsWithPendingCreate()

            assertTrue(result.isEmpty())
        }

    // --- Delete 404 handling ---

    @Test
    fun `processDeleteOperation treats 404 as success`() =
        runTest(testDispatcher) {
            val manager = createManager()

            val operation =
                PendingOperationEntity(
                    id = 4,
                    accountId = "account-1",
                    taskId = "task-1",
                    operationType = PendingOperationEntity.OPERATION_DELETE,
                    payload = createDeletePayloadJson("task-1", "list-1/task-1.ics", "etag-1"),
                    createdAt = Instant.now(),
                )

            coEvery { pendingOperationsDao.getPendingOperations("account-1") } returns listOf(operation)
            coEvery { calDavService.deleteTodo(any(), any(), any()) } returns
                Result.failure(IOException("404 Not Found"))

            manager.processPendingOperations()

            // Should delete the operation (treated as success since resource is gone)
            coVerify { pendingOperationsDao.delete(4) }
        }

    // --- Payload serialization helpers ---

    private fun createUpdatePayloadJson(task: TaskEntity): String {
        val payload =
            TaskPayload(
                id = task.id,
                accountId = task.accountId,
                listId = task.listId,
                title = task.title,
                description = task.description,
                completed = task.completed,
                due = task.due,
                updatedAt = task.updatedAt,
                priority = task.priority,
                status = task.status,
                completedAt = task.completedAt,
                uid = task.uid,
                etag = task.etag,
                href = task.href,
                parentUid = task.parentUid,
            )
        return moshi.adapter(TaskPayload::class.java).toJson(payload)
    }

    private fun createDeletePayloadJson(
        taskId: String,
        href: String?,
        etag: String?,
    ): String {
        val payload = DeletePayload(taskId = taskId, href = href, etag = etag)
        return moshi.adapter(DeletePayload::class.java).toJson(payload)
    }

    private fun createCreatePayloadJson(
        id: String,
        uid: String,
    ): String {
        val payload =
            CreatePayload(
                id = id,
                accountId = "account-1",
                listId = "list-1",
                title = "Test Task",
                description = "Description",
                completed = false,
                due = null,
                updatedAt = Instant.ofEpochMilli(1700000000000L),
                priority = null,
                status = "NEEDS-ACTION",
                completedAt = null,
                uid = uid,
            )
        return moshi.adapter(CreatePayload::class.java).toJson(payload)
    }

    companion object {
        private val moshi =
            com.squareup.moshi.Moshi
                .Builder()
                .add(
                    com.squareup.moshi.kotlin.reflect
                        .KotlinJsonAdapterFactory(),
                ).add(InstantAdapter())
                .build()
    }
}
