package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.repository.TasksRepository

class SyncTasksOnStartUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke() = repository.syncOnAppStart()
}
