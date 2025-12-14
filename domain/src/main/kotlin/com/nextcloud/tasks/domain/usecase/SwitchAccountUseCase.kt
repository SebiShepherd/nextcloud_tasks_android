package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.repository.AccountRepository

class SwitchAccountUseCase(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(accountId: String): Result<Unit> {
        if (accountId.isBlank()) return Result.failure(IllegalArgumentException("Account id is missing"))
        return runCatching { repository.setActiveAccount(accountId) }
    }
}
