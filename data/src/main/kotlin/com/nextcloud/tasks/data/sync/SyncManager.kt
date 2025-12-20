package com.nextcloud.tasks.data.sync

import com.nextcloud.tasks.domain.repository.TasksRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class SyncManager
    @Inject
    constructor(
        private val tasksRepository: TasksRepository,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun onAppStart() {
            scope.launch { pullWithRetry() }
        }

        suspend fun manualRefresh() {
            pullWithRetry()
        }

        private suspend fun pullWithRetry(maxAttempts: Int = 3, baseDelayMs: Long = 1_000L) {
            var delayMs = baseDelayMs
            repeat(maxAttempts) { attempt ->
                runCatching { tasksRepository.refresh() }
                    .onSuccess { return }
                    .onFailure { throwable ->
                        if (attempt == maxAttempts - 1) {
                            throw throwable
                        }
                        delay(delayMs)
                        delayMs *= 2
                    }
            }
        }
    }
