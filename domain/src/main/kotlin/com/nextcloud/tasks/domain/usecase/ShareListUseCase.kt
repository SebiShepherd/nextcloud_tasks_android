package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.ShareAccess
import com.nextcloud.tasks.domain.model.ShareeType
import com.nextcloud.tasks.domain.repository.TasksRepository

class ShareListUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(
        listId: String,
        shareeId: String,
        type: ShareeType,
        access: ShareAccess,
    ) = repository.shareList(listId, shareeId, type, access)
}
