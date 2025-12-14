package com.nextcloud.tasks.domain.repository

import com.nextcloud.tasks.domain.model.NextcloudAccount
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun loginWithPassword(serverUrl: String, username: String, password: String): NextcloudAccount

    suspend fun loginWithOAuth(serverUrl: String, authorizationCode: String, redirectUri: String): NextcloudAccount

    fun observeAccounts(): Flow<List<NextcloudAccount>>

    fun observeActiveAccount(): Flow<NextcloudAccount?>

    suspend fun switchAccount(accountId: String)

    suspend fun logout(accountId: String)
}
