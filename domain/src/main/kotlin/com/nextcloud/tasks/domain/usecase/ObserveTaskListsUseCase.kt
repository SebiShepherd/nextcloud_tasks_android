package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.TaskList
import com.nextcloud.tasks.domain.repository.TasksRepository
import kotlinx.coroutines.flow.Flow

class ObserveTaskListsUseCase(
    private val repository: TasksRepository,
) {
    operator fun invoke(): Flow<List<TaskList>> = repository.observeLists()
}
