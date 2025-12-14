package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.Account
import com.nextcloud.tasks.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class ObserveAccountsUseCase(
    private val repository: AccountRepository,
) {
    operator fun invoke(): Flow<List<Account>> = repository.accounts
}
