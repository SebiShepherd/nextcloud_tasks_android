package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.repository.TasksRepository
import kotlinx.coroutines.flow.Flow

class LoadTasksUseCase(private val repository: TasksRepository) {
    operator fun invoke(): Flow<List<Task>> = repository.observeTasks()

    suspend fun seedSample() = repository.addSampleTasksIfEmpty()
}
