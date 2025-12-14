package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.repository.AuthRepository

class LogoutUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(accountId: String) {
        repository.logout(accountId)
    }
}
