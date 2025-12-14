package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveActiveAccountUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<NextcloudAccount?> = repository.observeActiveAccount()
}
