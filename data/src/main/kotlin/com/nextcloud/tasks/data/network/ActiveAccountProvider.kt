package com.nextcloud.tasks.data.network

import com.nextcloud.tasks.domain.model.Account
import com.nextcloud.tasks.domain.repository.AccountRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
class ActiveAccountProvider @Inject constructor(accountRepository: AccountRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeAccountState: StateFlow<Account?> =
        accountRepository
            .activeAccount
            .stateIn(scope, SharingStarted.Eagerly, null)

    val current: Account?
        get() = activeAccountState.value
}
