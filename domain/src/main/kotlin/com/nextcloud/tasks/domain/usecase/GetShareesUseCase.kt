package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.Sharee
import com.nextcloud.tasks.domain.repository.TasksRepository

class GetShareesUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(listId: String): List<Sharee> = repository.getSharees(listId)
}
