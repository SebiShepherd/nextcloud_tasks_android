package com.nextcloud.tasks.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.tasks.domain.repository.TasksRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * WorkManager worker that performs periodic background synchronization
 * of tasks with the Nextcloud server.
 */
@HiltWorker
class SyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val tasksRepository: TasksRepository,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            Timber.d("SyncWorker: Starting background sync")
            return try {
                tasksRepository.refresh()
                Timber.d("SyncWorker: Background sync completed successfully")
                Result.success()
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Timber.e(e, "SyncWorker: Background sync failed")
                Result.retry()
            }
        }

        companion object {
            const val WORK_NAME = "periodic_sync"
        }
    }
