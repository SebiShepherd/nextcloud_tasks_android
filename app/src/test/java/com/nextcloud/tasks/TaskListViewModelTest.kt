package com.nextcloud.tasks

import com.nextcloud.tasks.data.caldav.service.CalDavHttpException
import com.nextcloud.tasks.domain.model.AuthType
import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskFilter
import com.nextcloud.tasks.domain.model.TaskSort
import com.nextcloud.tasks.domain.repository.TasksRepository
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.domain.usecase.ObserveActiveAccountUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val now = Instant.ofEpochMilli(1700000000000L)

    @Suppress("LongParameterList")
    private fun createTask(
        id: String = "task-1",
        listId: String = "list-1",
        title: String = "Test Task",
        description: String? = null,
        completed: Boolean = false,
        due: Instant? = null,
        priority: Int? = null,
    ) = Task(
        id = id,
        listId = listId,
        title = title,
        description = description,
        completed = completed,
        due = due,
        updatedAt = now,
        priority = priority,
    )

    private fun withViewModel(
        tasks: List<Task> = emptyList(),
        accountFlow: kotlinx.coroutines.flow.Flow<NextcloudAccount?> = flowOf(null),
        block: (TaskListViewModel) -> Unit,
    ) {
        Dispatchers.setMain(testDispatcher)
        try {
            val tasksRepository = mockk<TasksRepository>(relaxed = true)
            val loadTasksUseCase = mockk<LoadTasksUseCase>()
            val observeActiveAccountUseCase = mockk<ObserveActiveAccountUseCase>()

            every { loadTasksUseCase() } returns flowOf(tasks)
            every { tasksRepository.observeLists() } returns flowOf(emptyList())
            every { tasksRepository.observeIsOnline() } returns flowOf(true)
            every { tasksRepository.observeHasPendingChanges() } returns flowOf(false)
            every { observeActiveAccountUseCase() } returns accountFlow

            val vm =
                TaskListViewModel(
                    loadTasksUseCase = loadTasksUseCase,
                    tasksRepository = tasksRepository,
                    observeActiveAccountUseCase = observeActiveAccountUseCase,
                )
            block(vm)
        } finally {
            Dispatchers.resetMain()
        }
    }

    /** Variant that also exposes the mocked repository for verification. */
    private fun withViewModelAndRepo(
        tasks: List<Task> = emptyList(),
        block: (TaskListViewModel, TasksRepository) -> Unit,
    ) {
        Dispatchers.setMain(testDispatcher)
        try {
            val tasksRepository = mockk<TasksRepository>(relaxed = true)
            val loadTasksUseCase = mockk<LoadTasksUseCase>()
            val observeActiveAccountUseCase = mockk<ObserveActiveAccountUseCase>()

            every { loadTasksUseCase() } returns flowOf(tasks)
            every { tasksRepository.observeLists() } returns flowOf(emptyList())
            every { tasksRepository.observeIsOnline() } returns flowOf(true)
            every { tasksRepository.observeHasPendingChanges() } returns flowOf(false)
            every { observeActiveAccountUseCase() } returns flowOf(null)

            val vm =
                TaskListViewModel(
                    loadTasksUseCase = loadTasksUseCase,
                    tasksRepository = tasksRepository,
                    observeActiveAccountUseCase = observeActiveAccountUseCase,
                )
            block(vm, tasksRepository)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // --- selectList ---

    @Test
    fun `selectList updates selectedListId`() {
        withViewModel {
            it.selectList("list-1")
            assertEquals("list-1", it.selectedListId.value)

            it.selectList(null)
            assertNull(it.selectedListId.value)
        }
    }

    // --- setFilter ---

    @Test
    fun `setFilter updates taskFilter`() {
        withViewModel {
            it.setFilter(TaskFilter.COMPLETED)
            assertEquals(TaskFilter.COMPLETED, it.taskFilter.value)

            it.setFilter(TaskFilter.CURRENT)
            assertEquals(TaskFilter.CURRENT, it.taskFilter.value)
        }
    }

    // --- setSort ---

    @Test
    fun `setSort updates taskSort`() {
        withViewModel {
            it.setSort(TaskSort.TITLE)
            assertEquals(TaskSort.TITLE, it.taskSort.value)

            it.setSort(TaskSort.PRIORITY)
            assertEquals(TaskSort.PRIORITY, it.taskSort.value)
        }
    }

    // --- setSearchQuery ---

    @Test
    fun `setSearchQuery updates searchQuery`() {
        withViewModel {
            it.setSearchQuery("test")
            assertEquals("test", it.searchQuery.value)
        }
    }

    // --- filtering tasks ---

    @Test
    fun `tasks are filtered by selected list`() =
        runTest(testDispatcher) {
            val tasks =
                listOf(
                    createTask(id = "1", listId = "list-1"),
                    createTask(id = "2", listId = "list-2"),
                )
            withViewModel(tasks = tasks) { vm ->
                vm.selectList("list-1")
                val result = vm.tasks.first()
                assertEquals(1, result.size)
                assertEquals("1", result[0].id)
            }
        }

    @Test
    fun `tasks are filtered by completion status`() =
        runTest(testDispatcher) {
            val tasks =
                listOf(
                    createTask(id = "1", completed = false),
                    createTask(id = "2", completed = true),
                )
            withViewModel(tasks = tasks) { vm ->
                vm.setFilter(TaskFilter.CURRENT)
                val current = vm.tasks.first()
                assertEquals(1, current.size)
                assertEquals("1", current[0].id)

                vm.setFilter(TaskFilter.COMPLETED)
                val completed = vm.tasks.first()
                assertEquals(1, completed.size)
                assertEquals("2", completed[0].id)
            }
        }

    @Test
    fun `tasks are filtered by search query case-insensitively`() =
        runTest(testDispatcher) {
            val tasks =
                listOf(
                    createTask(id = "1", title = "Buy Groceries"),
                    createTask(id = "2", title = "Read Book"),
                    createTask(id = "3", title = "Buy Milk", description = "grocery store"),
                )
            withViewModel(tasks = tasks) { vm ->
                vm.setSearchQuery("buy")
                val result = vm.tasks.first()
                assertEquals(2, result.size)
            }
        }

    @Test
    fun `search query matches description`() =
        runTest(testDispatcher) {
            val tasks =
                listOf(
                    createTask(id = "1", title = "Task", description = "important note"),
                )
            withViewModel(tasks = tasks) { vm ->
                vm.setSearchQuery("important")
                val result = vm.tasks.first()
                assertEquals(1, result.size)
            }
        }

    // --- sorting ---

    @Test
    fun `tasks sorted by title`() =
        runTest(testDispatcher) {
            val tasks =
                listOf(
                    createTask(id = "1", title = "Charlie"),
                    createTask(id = "2", title = "Alpha"),
                    createTask(id = "3", title = "Bravo"),
                )
            withViewModel(tasks = tasks) { vm ->
                vm.setSort(TaskSort.TITLE)
                val result = vm.tasks.first()
                assertEquals("Alpha", result[0].title)
                assertEquals("Bravo", result[1].title)
                assertEquals("Charlie", result[2].title)
            }
        }

    @Test
    fun `tasks sorted by due date`() =
        runTest(testDispatcher) {
            val early = Instant.ofEpochMilli(1000L)
            val late = Instant.ofEpochMilli(2000L)
            val tasks =
                listOf(
                    createTask(id = "1", due = late),
                    createTask(id = "2", due = early),
                    createTask(id = "3", due = null),
                )
            withViewModel(tasks = tasks) { vm ->
                vm.setSort(TaskSort.DUE_DATE)
                val result = vm.tasks.first()
                assertEquals(early, result[0].due)
                assertEquals(late, result[1].due)
                assertNull(result[2].due)
            }
        }

    @Test
    fun `tasks sorted by updated at descending`() =
        runTest(testDispatcher) {
            val early = Instant.ofEpochMilli(1000L)
            val late = Instant.ofEpochMilli(2000L)
            val tasks =
                listOf(
                    createTask(id = "1").copy(updatedAt = early),
                    createTask(id = "2").copy(updatedAt = late),
                )
            withViewModel(tasks = tasks) { vm ->
                vm.setSort(TaskSort.UPDATED_AT)
                val result = vm.tasks.first()
                assertEquals(late, result[0].updatedAt)
                assertEquals(early, result[1].updatedAt)
            }
        }

    // --- refresh ---

    @Test
    fun `refresh calls repository refresh`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } returns Unit
                vm.refresh()
                coVerify { repo.refresh() }
            }
        }

    @Test
    fun `refresh sets isRefreshing during refresh`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } returns Unit
                vm.refresh()
                assertFalse(vm.isRefreshing.value)
            }
        }

    @Test
    fun `refresh sets NETWORK_ERROR on UnknownHostException`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } throws java.net.UnknownHostException("dns")
                vm.refresh()
                assertEquals(RefreshError.NETWORK_ERROR, vm.refreshError.value)
            }
        }

    @Test
    fun `refresh sets NETWORK_ERROR on ConnectException`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } throws java.net.ConnectException("refused")
                vm.refresh()
                assertEquals(RefreshError.NETWORK_ERROR, vm.refreshError.value)
            }
        }

    @Test
    fun `refresh sets NETWORK_ERROR on SocketTimeoutException`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } throws java.net.SocketTimeoutException("timeout")
                vm.refresh()
                assertEquals(RefreshError.NETWORK_ERROR, vm.refreshError.value)
            }
        }

    @Test
    fun `refresh sets UNKNOWN on generic exception`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } throws RuntimeException("oops")
                vm.refresh()
                assertEquals(RefreshError.UNKNOWN, vm.refreshError.value)
            }
        }

    @Test
    fun `clearRefreshError resets error`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } throws RuntimeException("oops")
                vm.refresh()
                assertEquals(RefreshError.UNKNOWN, vm.refreshError.value)

                vm.clearRefreshError()
                assertNull(vm.refreshError.value)
            }
        }

    // --- CalDavHttpException handling ---

    @Test
    fun `refresh sets RATE_LIMITED on 429 CalDavHttpException`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } throws CalDavHttpException(429, "Too Many Requests")
                vm.refresh()
                assertEquals(RefreshError.RATE_LIMITED, vm.refreshError.value)
            }
        }

    @Test
    fun `refresh sets AUTH_FAILED on 401 CalDavHttpException`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } throws CalDavHttpException(401, "Unauthorized")
                vm.refresh()
                assertEquals(RefreshError.AUTH_FAILED, vm.refreshError.value)
            }
        }

    @Test
    fun `refresh sets AUTH_FAILED on 403 CalDavHttpException`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } throws CalDavHttpException(403, "Forbidden")
                vm.refresh()
                assertEquals(RefreshError.AUTH_FAILED, vm.refreshError.value)
            }
        }

    @Test
    fun `refresh sets SERVER_ERROR on 500 CalDavHttpException`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } throws CalDavHttpException(500, "Internal Server Error")
                vm.refresh()
                assertEquals(RefreshError.SERVER_ERROR, vm.refreshError.value)
            }
        }

    @Test
    fun `refresh sets SERVER_ERROR on other HTTP error CalDavHttpException`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.refresh() } throws CalDavHttpException(502, "Bad Gateway")
                vm.refresh()
                assertEquals(RefreshError.SERVER_ERROR, vm.refreshError.value)
            }
        }

    // --- createTask ---

    @Test
    fun `createTask calls repository`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                coEvery { repo.createTask(any()) } returns createTask()
                vm.createTask("New Task", "Description", "list-1")
                coVerify {
                    repo.createTask(
                        match {
                            it.title == "New Task" && it.description == "Description" && it.listId == "list-1"
                        },
                    )
                }
            }
        }

    // --- toggleTaskComplete ---

    @Test
    fun `toggleTaskComplete flips completed state`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                val task = createTask(completed = false)
                coEvery { repo.updateTask(any()) } returns task.copy(completed = true)
                vm.toggleTaskComplete(task)
                coVerify {
                    repo.updateTask(match { it.completed && it.status == "COMPLETED" })
                }
            }
        }

    @Test
    fun `toggleTaskComplete uncompletes a completed task`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                val task = createTask(completed = true)
                coEvery { repo.updateTask(any()) } returns task.copy(completed = false)
                vm.toggleTaskComplete(task)
                coVerify {
                    repo.updateTask(match { !it.completed && it.status == "NEEDS-ACTION" })
                }
            }
        }

    // --- deleteTask ---

    @Test
    fun `deleteTask calls repository`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                vm.deleteTask("task-1")
                coVerify { repo.deleteTask("task-1") }
            }
        }

    // --- animatingEntryTaskIds ---

    @Test
    fun `clearAnimatingEntryTaskId removes task id`() =
        runTest(testDispatcher) {
            withViewModelAndRepo { vm, repo ->
                val task = createTask(id = "task-1")
                coEvery { repo.updateTask(any()) } returns task

                vm.toggleTaskComplete(task)
                assertTrue(vm.animatingEntryTaskIds.value.contains("task-1"))

                vm.clearAnimatingEntryTaskId("task-1")
                assertFalse(vm.animatingEntryTaskIds.value.contains("task-1"))
            }
        }

    // --- account switching resets selected list ---

    @Test
    fun `account change resets selected list`() =
        runTest(testDispatcher) {
            val accountFlow = MutableSharedFlow<NextcloudAccount?>()

            withViewModel(accountFlow = accountFlow) { vm ->
                vm.selectList("list-1")

                val account1 =
                    NextcloudAccount("acc-1", "User 1", "https://a.com", "user1", AuthType.PASSWORD)
                val account2 =
                    NextcloudAccount("acc-2", "User 2", "https://b.com", "user2", AuthType.PASSWORD)

                accountFlow.emit(account1)
                assertEquals("list-1", vm.selectedListId.value)

                accountFlow.emit(account2)
                assertNull(vm.selectedListId.value)
            }
        }

    // --- default values ---

    @Test
    fun `default filter is ALL`() {
        withViewModel { assertEquals(TaskFilter.ALL, it.taskFilter.value) }
    }

    @Test
    fun `default sort is DUE_DATE`() {
        withViewModel { assertEquals(TaskSort.DUE_DATE, it.taskSort.value) }
    }

    @Test
    fun `default search query is empty`() {
        withViewModel { assertEquals("", it.searchQuery.value) }
    }
}
