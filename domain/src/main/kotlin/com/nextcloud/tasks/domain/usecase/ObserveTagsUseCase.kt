package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.repository.TasksRepository
import kotlinx.coroutines.flow.Flow

class ObserveTagsUseCase(
    private val repository: TasksRepository,
) {
    operator fun invoke(): Flow<List<Tag>> = repository.observeTags()
}
