package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.repository.TasksRepository

class DeleteTaskUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(taskId: String) = repository.deleteTask(taskId)
}
