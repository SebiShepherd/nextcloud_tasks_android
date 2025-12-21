package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.repository.TasksRepository

class UpdateTaskUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(task: Task): Task = repository.updateTask(task)
}
