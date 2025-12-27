package com.nextcloud.tasks.data.auth

import kotlinx.coroutines.flow.Flow

interface AuthTokenProvider {
    fun activeToken(): AuthToken?

    fun activeServerUrl(): String?

    fun activeAccountId(): String?

    fun observeActiveAccountId(): Flow<String?>
}
