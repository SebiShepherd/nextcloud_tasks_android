package com.nextcloud.tasks.sync

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.nextcloud.tasks.domain.repository.TasksRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncWorkerTest {
    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val tasksRepository = mockk<TasksRepository>(relaxed = true)

    private fun createWorker() = SyncWorker(context, workerParams, tasksRepository)

    @Test
    fun `doWork returns success when refresh succeeds`() =
        runTest {
            val worker = createWorker()
            coEvery { tasksRepository.refresh() } returns Unit

            val result = worker.doWork()

            assertEquals(Result.success(), result)
            coVerify(exactly = 1) { tasksRepository.refresh() }
        }

    @Test
    fun `doWork returns retry when refresh throws exception`() =
        runTest {
            val worker = createWorker()
            coEvery { tasksRepository.refresh() } throws RuntimeException("Network error")

            val result = worker.doWork()

            assertEquals(Result.retry(), result)
        }

    @Test
    fun `WORK_NAME constant is periodic_sync`() {
        assertEquals("periodic_sync", SyncWorker.WORK_NAME)
    }
}
