package com.nextcloud.tasks.detail

import androidx.lifecycle.SavedStateHandle
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.repository.TasksRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import java.time.Instant
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val now = Instant.ofEpochMilli(1_700_000_000_000L)
    private val taskId = "t1"

    private val task =
        Task(
            id = taskId,
            listId = "l1",
            title = "Title",
            description = "original note",
            updatedAt = now,
        )

    private fun repo(): TasksRepository =
        mockk(relaxed = true) {
            coEvery { getTask(taskId) } returns task
            every { observeTags() } returns flowOf(emptyList())
            every { observeLists() } returns flowOf(emptyList())
            every { observeTasks() } returns flowOf(emptyList())
            coEvery { updateTask(any()) } returns task
        }

    private fun viewModel(repository: TasksRepository) =
        TaskDetailViewModel(
            repository,
            CoroutineScope(testDispatcher),
            SavedStateHandle(mapOf("taskId" to taskId)),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveDescriptionNow does not persist when the description is unchanged`() =
        runTest {
            val repository = repo()
            val vm = viewModel(repository)

            vm.saveDescriptionNow("original note")

            coVerify(exactly = 0) { repository.updateTask(any()) }
        }

    @Test
    fun `saveDescriptionNow persists when the description actually changed`() =
        runTest {
            val repository = repo()
            val vm = viewModel(repository)

            vm.saveDescriptionNow("edited note")

            coVerify(exactly = 1) { repository.updateTask(any()) }
        }

    @Test
    fun `addSubtask creates a task in the same list`() =
        runTest {
            val repository = repo()
            val vm = viewModel(repository)

            vm.addSubtask("child")

            coVerify(exactly = 1) { repository.createTask(match { it.title == "child" && it.listId == "l1" }) }
        }

    @Test
    fun `addSubtask ignores a blank title`() =
        runTest {
            val repository = repo()
            val vm = viewModel(repository)

            vm.addSubtask("   ")

            coVerify(exactly = 0) { repository.createTask(any()) }
        }
}
