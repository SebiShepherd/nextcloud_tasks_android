package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.ShareeType
import com.nextcloud.tasks.domain.repository.TasksRepository

class UnshareListUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(
        listId: String,
        shareeId: String,
        type: ShareeType,
    ) = repository.unshareList(listId, shareeId, type)
}
