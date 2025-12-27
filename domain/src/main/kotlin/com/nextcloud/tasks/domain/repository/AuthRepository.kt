package com.nextcloud.tasks.domain.repository

import com.nextcloud.tasks.domain.model.LoginFlowV2Initiation
import com.nextcloud.tasks.domain.model.LoginFlowV2PollResult
import com.nextcloud.tasks.domain.model.NextcloudAccount
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /**
     * Initiates the Login Flow v2 process.
     * Returns the login URL to open in browser and the polling endpoint.
     */
    suspend fun initiateLoginFlowV2(serverUrl: String): LoginFlowV2Initiation

    /**
     * Polls the Login Flow v2 endpoint to check if authentication is complete.
     * Returns Pending if user hasn't completed login, Success with credentials if done, or Error if failed.
     */
    suspend fun pollLoginFlowV2(
        pollUrl: String,
        token: String,
    ): LoginFlowV2PollResult

    /**
     * Completes login with an app password obtained from Login Flow v2.
     * Creates a new account and sets it as active.
     */
    suspend fun loginWithAppPassword(
        serverUrl: String,
        loginName: String,
        appPassword: String,
    ): NextcloudAccount

    /**
     * Imports an account from the Nextcloud Files app using Android SingleSignOn.
     * Requires the Nextcloud Files app to be installed.
     *
     * @param accountName The account name from Android's AccountManager
     * @return The imported NextcloudAccount
     */
    suspend fun importAccountFromFilesApp(accountName: String): NextcloudAccount

    /**
     * @deprecated Use Login Flow v2 instead. Will be removed in a future version.
     */
    @Deprecated("Use Login Flow v2", ReplaceWith("initiateLoginFlowV2(serverUrl)"))
    suspend fun loginWithPassword(
        serverUrl: String,
        username: String,
        password: String,
    ): NextcloudAccount

    /**
     * @deprecated Use Login Flow v2 instead. Will be removed in a future version.
     */
    @Deprecated("Use Login Flow v2", ReplaceWith("initiateLoginFlowV2(serverUrl)"))
    suspend fun loginWithOAuth(
        serverUrl: String,
        authorizationCode: String,
        redirectUri: String,
    ): NextcloudAccount

    fun observeAccounts(): Flow<List<NextcloudAccount>>

    fun observeActiveAccount(): Flow<NextcloudAccount?>

    suspend fun switchAccount(accountId: String)

    suspend fun logout(accountId: String)
}
