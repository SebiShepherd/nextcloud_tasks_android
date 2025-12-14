package com.nextcloud.tasks.domain.repository

import com.nextcloud.tasks.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    val accounts: Flow<List<Account>>
    val activeAccount: Flow<Account?>

    suspend fun saveAccount(account: Account)
    suspend fun setActiveAccount(accountId: String)
    suspend fun removeAccount(accountId: String)
    suspend fun clearActiveAccount()
}
