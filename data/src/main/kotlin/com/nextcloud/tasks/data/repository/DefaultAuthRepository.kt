package com.nextcloud.tasks.data.repository

import android.net.Uri
import com.nextcloud.tasks.data.auth.SecureAuthStorage
import com.nextcloud.tasks.data.auth.StoredAccount
import com.nextcloud.tasks.data.network.NextcloudClientFactory
import com.nextcloud.tasks.domain.model.AuthFailure
import com.nextcloud.tasks.domain.model.AuthType
import com.nextcloud.tasks.domain.model.LoginFlowV2Credentials
import com.nextcloud.tasks.domain.model.LoginFlowV2Initiation
import com.nextcloud.tasks.domain.model.LoginFlowV2PollResult
import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.repository.AuthRepository
import com.nextcloud.tasks.domain.usecase.ValidateServerUrlUseCase
import com.nextcloud.tasks.domain.usecase.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import timber.log.Timber
import java.net.UnknownHostException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

@Singleton
class DefaultAuthRepository
    @Inject
    constructor(
        private val clientFactory: NextcloudClientFactory,
        private val secureAuthStorage: SecureAuthStorage,
        private val validateServerUrlUseCase: ValidateServerUrlUseCase,
        private val tasksRepository: com.nextcloud.tasks.domain.repository.TasksRepository,
    ) : AuthRepository {
        private val oauthClientId = "nextcloud-tasks-android"
        private val oauthClientSecret = "local-client-secret"

        @Deprecated("Use Login Flow v2", ReplaceWith("initiateLoginFlowV2(serverUrl)"))
        override suspend fun loginWithPassword(
            serverUrl: String,
            username: String,
            password: String,
        ): NextcloudAccount {
            val normalizedServer = normalizeOrThrow(serverUrl)
            if (username.isBlank() || password.isBlank()) {
                throw AuthFailure.InvalidCredentials
            }

            val service = clientFactory.createWithBasicAuth(normalizedServer, username.trim(), password)
            val user = runCatching { service.fetchUser() }.getOrElse { throw mapError(it) }
            val account =
                StoredAccount(
                    id = UUID.randomUUID().toString(),
                    serverUrl = normalizedServer,
                    username = username.trim(),
                    displayName = user.body.data.displayName ?: username,
                    authType = AuthType.PASSWORD.name,
                    appPassword = password,
                )
            secureAuthStorage.saveAccount(account)
            secureAuthStorage.setActiveAccount(account.id)
            return account.toDomain()
        }

        @Deprecated("Use Login Flow v2", ReplaceWith("initiateLoginFlowV2(serverUrl)"))
        override suspend fun loginWithOAuth(
            serverUrl: String,
            authorizationCode: String,
            redirectUri: String,
        ): NextcloudAccount {
            val normalizedServer = normalizeOrThrow(serverUrl)
            if (authorizationCode.isBlank() || redirectUri.isBlank()) {
                throw AuthFailure.InvalidCredentials
            }

            val unauthenticatedService = clientFactory.createUnauthenticated(normalizedServer)
            val tokenResponse =
                runCatching {
                    unauthenticatedService.exchangeOAuthToken(
                        grantType = "authorization_code",
                        code = authorizationCode.trim(),
                        redirectUri = redirectUri.trim(),
                        clientId = oauthClientId,
                        clientSecret = oauthClientSecret,
                    )
                }.getOrElse { throw mapError(it) }

            val authenticatedService =
                clientFactory.createWithBearerToken(
                    normalizedServer,
                    token = tokenResponse.accessToken,
                )
            val user = runCatching { authenticatedService.fetchUser() }.getOrElse { throw mapError(it) }
            val account =
                StoredAccount(
                    id = UUID.randomUUID().toString(),
                    serverUrl = normalizedServer,
                    username =
                        user.body.data.id
                            .orEmpty(),
                    displayName = user.body.data.displayName ?: user.body.data.email ?: "Nextcloud user",
                    authType = AuthType.OAUTH.name,
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                )
            secureAuthStorage.saveAccount(account)
            secureAuthStorage.setActiveAccount(account.id)
            return account.toDomain()
        }

        override suspend fun initiateLoginFlowV2(serverUrl: String): LoginFlowV2Initiation {
            val normalized = normalizeOrThrow(serverUrl)
            Timber.d("Initiating Login Flow v2 for server: %s", normalized)

            val service = clientFactory.createUnauthenticated(normalized)
            val response =
                runCatching {
                    service.initiateLoginFlowV2()
                }.getOrElse { throwable ->
                    Timber.e(throwable, "Failed to initiate Login Flow v2")
                    throw mapError(throwable)
                }

            if (!response.isSuccessful || response.body() == null) {
                val errorMsg = "Failed to initiate login flow: HTTP ${response.code()}"
                Timber.e(errorMsg)
                throw AuthFailure.Network(errorMsg)
            }

            val body = response.body()!!
            Timber.d("Login Flow v2 initiated successfully. Poll URL: %s", body.poll.endpoint)

            return LoginFlowV2Initiation(
                pollUrl = body.poll.endpoint,
                loginUrl = body.login,
                token = body.poll.token,
            )
        }

        override suspend fun pollLoginFlowV2(
            pollUrl: String,
            token: String,
        ): LoginFlowV2PollResult {
            // Extract server URL from poll URL for creating client
            val serverUrl = extractServerUrl(pollUrl)
            val service = clientFactory.createUnauthenticated(serverUrl)

            val response =
                runCatching {
                    service.pollLoginFlowV2(pollUrl, token)
                }.getOrElse { throwable ->
                    // Network errors during polling should be treated as pending, not errors
                    Timber.w(throwable, "Error during Login Flow v2 polling, treating as pending")
                    return LoginFlowV2PollResult.Pending
                }

            return when (response.code()) {
                404 -> {
                    // Authentication still pending
                    Timber.v("Login Flow v2 poll: still pending (404)")
                    LoginFlowV2PollResult.Pending
                }

                200 -> {
                    val body = response.body()
                    if (body == null) {
                        val errorMsg = "Login Flow v2 poll: success but empty response"
                        Timber.e(errorMsg)
                        return LoginFlowV2PollResult.Error(errorMsg)
                    }

                    Timber.i("Login Flow v2 poll: authentication successful for %s", body.loginName)
                    LoginFlowV2PollResult.Success(
                        LoginFlowV2Credentials(
                            server = body.server,
                            loginName = body.loginName,
                            appPassword = body.appPassword,
                        ),
                    )
                }

                else -> {
                    val errorMsg = "Login Flow v2 poll: unexpected HTTP ${response.code()}"
                    Timber.e(errorMsg)
                    LoginFlowV2PollResult.Error(errorMsg)
                }
            }
        }

        override suspend fun loginWithAppPassword(
            serverUrl: String,
            loginName: String,
            appPassword: String,
        ): NextcloudAccount {
            val normalizedServer = normalizeOrThrow(serverUrl)
            Timber.d("Completing Login Flow v2 with app password for %s", loginName)

            if (loginName.isBlank() || appPassword.isBlank()) {
                throw AuthFailure.InvalidCredentials
            }

            // Check for duplicate account (same server + username)
            val existingAccounts = secureAuthStorage.observeAccounts().first()
            val duplicateAccount =
                existingAccounts.firstOrNull { existing ->
                    existing.serverUrl == normalizedServer && existing.username == loginName
                }
            if (duplicateAccount != null) {
                Timber.w("Account already exists for %s@%s", loginName, normalizedServer)
                // Instead of creating a new account, switch to the existing one
                secureAuthStorage.setActiveAccount(duplicateAccount.id)
                return duplicateAccount.toDomain()
            }

            // Authenticate with app password (using Basic Auth)
            val service = clientFactory.createWithBasicAuth(normalizedServer, loginName, appPassword)
            val user = runCatching { service.fetchUser() }.getOrElse { throw mapError(it) }

            val account =
                StoredAccount(
                    id = UUID.randomUUID().toString(),
                    serverUrl = normalizedServer,
                    username = loginName,
                    displayName = user.body.data.displayName ?: loginName,
                    authType = AuthType.PASSWORD.name, // Login Flow v2 provides app password (Basic Auth)
                    appPassword = appPassword,
                )

            secureAuthStorage.saveAccount(account)
            secureAuthStorage.setActiveAccount(account.id)

            Timber.i("Login Flow v2 completed successfully for account %s", account.id)
            return account.toDomain()
        }

        override suspend fun importAccountFromFilesApp(accountName: String): NextcloudAccount {
            Timber.d("Importing account from Files app: %s", accountName)

            // TODO: Implement full SingleSignOn integration
            // This is a placeholder implementation that provides the foundation
            // for importing accounts from the Nextcloud Files app.
            // Full implementation requires:
            // 1. Check if Nextcloud Files app is installed
            // 2. Use AccountImporter.pickNewAccount() to get account
            // 3. Extract account details (server URL, username, etc.)
            // 4. Store in SecureAuthStorage
            // 5. Set as active account

            throw AuthFailure.Unexpected(
                "Account import from Nextcloud Files app is not yet fully implemented. " +
                    "Please use the browser-based login flow instead.",
            )
        }

        override fun observeAccounts(): Flow<List<NextcloudAccount>> =
            secureAuthStorage.observeAccounts().map { stored -> stored.map { it.toDomain() } }

        override fun observeActiveAccount(): Flow<NextcloudAccount?> =
            combine(
                secureAuthStorage.observeAccounts(),
                secureAuthStorage.observeActiveAccountId(),
            ) { accounts, activeId ->
                accounts.firstOrNull { it.id == activeId }?.toDomain()
            }

        override suspend fun switchAccount(accountId: String) {
            if (secureAuthStorage.findAccount(accountId) == null) {
                throw AuthFailure.Unexpected("Account not found")
            }
            secureAuthStorage.setActiveAccount(accountId)
        }

        override suspend fun logout(accountId: String) {
            // Clear all tasks and lists for this account
            tasksRepository.clearAccountData(accountId)
            // Remove account from storage
            secureAuthStorage.removeAccount(accountId)
        }

        private fun normalizeOrThrow(serverUrl: String): String =
            when (val validation = validateServerUrlUseCase(serverUrl)) {
                is ValidationResult.Invalid -> throw AuthFailure.InvalidServerUrl(validation.reason)
                is ValidationResult.Valid -> validation.normalizedUrl
            }

        private fun mapError(throwable: Throwable): AuthFailure =
            when (throwable) {
                is HttpException ->
                    if (throwable.code() == 401) {
                        AuthFailure.InvalidCredentials
                    } else {
                        AuthFailure.Network("Server error (${throwable.code()})")
                    }

                is SecurityException ->
                    AuthFailure.Network("Network access denied (missing INTERNET permission?)")

                is UnknownHostException -> AuthFailure.Network("Server unreachable. Please check the URL")
                is SSLPeerUnverifiedException, is SSLHandshakeException ->
                    AuthFailure.Certificate("Certificate could not be verified")
                else -> AuthFailure.Unexpected(throwable.message ?: "Unknown error")
            }

        /**
         * Extracts the base server URL (scheme + host) from a full URL.
         * Example: "https://cloud.example.com/login/v2/poll" -> "https://cloud.example.com"
         */
        private fun extractServerUrl(pollUrl: String): String {
            val uri = Uri.parse(pollUrl)
            return "${uri.scheme}://${uri.host}"
        }
    }

private fun StoredAccount.toDomain(): NextcloudAccount =
    NextcloudAccount(
        id = id,
        displayName = displayName,
        serverUrl = serverUrl,
        username = username,
        authType = runCatching { AuthType.valueOf(authType) }.getOrElse { AuthType.PASSWORD },
    )
