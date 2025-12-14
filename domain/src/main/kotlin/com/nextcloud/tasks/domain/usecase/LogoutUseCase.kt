package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.repository.AccountRepository

class LogoutUseCase(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke() = repository.clearActiveAccount()
}
