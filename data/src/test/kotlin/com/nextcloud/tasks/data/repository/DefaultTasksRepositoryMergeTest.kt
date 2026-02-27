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
import com.nextcloud.tasks.data.mapper.TagMapper
import com.nextcloud.tasks.data.mapper.TaskListMapper
import com.nextcloud.tasks.data.mapper.TaskMapper
import com.nextcloud.tasks.data.network.NetworkMonitor
import com.nextcloud.tasks.data.sync.PendingOperationsManager
import com.nextcloud.tasks.data.sync.TaskFieldMerger
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
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTasksRepositoryMergeTest {
    private val tasksDao = mockk<TasksDao>(relaxed = true)
    private val taskListsDao = mockk<TaskListsDao>(relaxed = true)
    private val tagsDao = mockk<TagsDao>(relaxed = true)
    private val pendingOperationsDao = mockk<PendingOperationsDao>(relaxed = true)
    private val database =
        mockk<NextcloudTasksDatabase>(relaxed = true) {
            every { tasksDao() } returns this@DefaultTasksRepositoryMergeTest.tasksDao
            every { taskListsDao() } returns this@DefaultTasksRepositoryMergeTest.taskListsDao
            every { tagsDao() } returns this@DefaultTasksRepositoryMergeTest.tagsDao
            every { pendingOperationsDao() } returns this@DefaultTasksRepositoryMergeTest.pendingOperationsDao
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

    // --- clearAccountData ---

    @Test
    fun `clearAccountData deletes tasks and lists for account`() =
        runTest(testDispatcher) {
            val repo = createRepository()

            repo.clearAccountData("account-1")

            coVerify { tasksDao.deleteTasksByAccount("account-1") }
            coVerify { taskListsDao.deleteListsByAccount("account-1") }
        }

    // --- observeIsOnline delegates to networkMonitor ---

    @Test
    fun `observeIsOnline returns network monitor flow`() {
        val repo = createRepository()
        every { networkMonitor.isOnline } returns flowOf(true)

        val flow = repo.observeIsOnline()
        // Flow is the same reference as networkMonitor.isOnline
        assert(flow === networkMonitor.isOnline)
    }

    // --- observeHasPendingChanges delegates to pendingOperationsManager ---

    @Test
    fun `observeHasPendingChanges delegates to pending operations manager`() {
        val repo = createRepository()
        val pendingFlow = flowOf(false)
        every { pendingOperationsManager.hasPendingOperations() } returns pendingFlow

        val flow = repo.observeHasPendingChanges()
        assert(flow === pendingFlow)
    }

    // --- isCurrentlyOnline delegates to networkMonitor ---

    @Test
    fun `isCurrentlyOnline delegates to network monitor`() {
        val repo = createRepository()
        every { networkMonitor.isCurrentlyOnline() } returns true

        assert(repo.isCurrentlyOnline())
    }

    // --- refresh skips when no active server URL ---

    @Test
    fun `refresh skips when no active server URL`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            coEvery { authTokenProvider.activeServerUrl() } returns null

            repo.refresh()

            // Should not attempt CalDAV discovery
            coVerify(exactly = 0) { calDavService.discoverPrincipal(any()) }
        }

    @Test
    fun `refresh skips when no active account`() =
        runTest(testDispatcher) {
            val repo = createRepository()
            coEvery { authTokenProvider.activeServerUrl() } returns "https://cloud.example.com"
            coEvery { authTokenProvider.activeAccountId() } returns null

            repo.refresh()

            coVerify(exactly = 0) { calDavService.discoverPrincipal(any()) }
        }
}
