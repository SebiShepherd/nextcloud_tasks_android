package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskDraft
import com.nextcloud.tasks.domain.repository.TasksRepository

class CreateTaskUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(draft: TaskDraft): Task = repository.createTask(draft)
}
