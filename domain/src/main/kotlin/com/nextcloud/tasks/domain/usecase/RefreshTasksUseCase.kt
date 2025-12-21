package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.repository.TasksRepository

class RefreshTasksUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke() = repository.refresh()
}
