package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.repository.TasksRepository

class GetTaskUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(taskId: String): Task? = repository.getTask(taskId)
}
