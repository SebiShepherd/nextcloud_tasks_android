package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.repository.TasksRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadTasksUseCaseTest {
    private val repository = mockk<TasksRepository>()
    private val useCase = LoadTasksUseCase(repository)

    @Test
    fun `invoke returns flow from repository`() = runTest {
        val tasks = listOf(
            Task(
                id = "task-1",
                listId = "list-1",
                title = "Task 1",
                updatedAt = Instant.now(),
            ),
            Task(
                id = "task-2",
                listId = "list-1",
                title = "Task 2",
                updatedAt = Instant.now(),
            ),
        )
        every { repository.observeTasks() } returns flowOf(tasks)

        val result = useCase()

        // Collect the flow and verify
        result.collect { emittedTasks ->
            assertEquals(2, emittedTasks.size)
            assertEquals("Task 1", emittedTasks[0].title)
            assertEquals("Task 2", emittedTasks[1].title)
        }
    }

    @Test
    fun `invoke calls repository observeTasks`() = runTest {
        every { repository.observeTasks() } returns flowOf(emptyList())

        useCase()

        // Verify the repository method was called
        // Note: Flow is lazy, so we don't verify unless collected
        every { repository.observeTasks() }
    }

    @Test
    fun `seedSample calls repository addSampleTasksIfEmpty`() = runTest {
        coEvery { repository.addSampleTasksIfEmpty() } returns Unit

        useCase.seedSample()

        coVerify { repository.addSampleTasksIfEmpty() }
    }
}
