package com.nextcloud.tasks.data.sync

import com.nextcloud.tasks.domain.repository.TasksRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

class SyncManager @Inject constructor(
    private val tasksRepository: TasksRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun syncOnStart() {
        withRetry { tasksRepository.syncOnAppStart() }
    }

    suspend fun refreshNow() {
        withRetry { tasksRepository.refresh() }
    }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelayMillis: Long = 1_000,
        block: suspend () -> T,
    ): T {
        var attempt = 0
        var delayMillis = initialDelayMillis
        var lastError: Throwable? = null

        while (attempt < maxAttempts) {
            try {
                return withContext(ioDispatcher) { block() }
            } catch (error: Throwable) {
                lastError = error
                attempt++
                if (attempt >= maxAttempts) {
                    Timber.e(error, "Sync failed after %d attempts", attempt)
                    throw error
                }
                Timber.w(
                    error,
                    "Sync attempt %d failed, retrying in %d ms",
                    attempt,
                    delayMillis,
                )
                delay(delayMillis)
                delayMillis *= 2
            }
        }

        throw lastError ?: IllegalStateException("Sync failed without throwing an exception")
    }
}
