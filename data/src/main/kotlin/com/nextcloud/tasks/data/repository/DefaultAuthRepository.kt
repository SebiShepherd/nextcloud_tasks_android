package com.nextcloud.tasks.data.repository

import com.nextcloud.tasks.data.BuildConfig
import com.nextcloud.tasks.data.auth.SecureAuthStorage
import com.nextcloud.tasks.data.auth.StoredAccount
import com.nextcloud.tasks.data.network.NextcloudClientFactory
import com.nextcloud.tasks.domain.model.AuthFailure
import com.nextcloud.tasks.domain.model.AuthType
import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.repository.AuthRepository
import com.nextcloud.tasks.domain.usecase.ValidateServerUrlUseCase
import com.nextcloud.tasks.domain.usecase.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
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
    ) : AuthRepository {
        private val oauthClientId = BuildConfig.OAUTH_CLIENT_ID
        private val oauthClientSecret = BuildConfig.OAUTH_CLIENT_SECRET

        override suspend fun loginWithPassword(
            serverUrl: String,
            username: String,
            password: String,
        ): NextcloudAccount {
            val normalizedServer = normalizeOrThrow(serverUrl)
            val trimmedUsername = username.trim()
            val trimmedPassword = password.trim()
            if (trimmedUsername.isBlank() || trimmedPassword.isBlank()) {
                throw AuthFailure.InvalidCredentials
            }

            // Check for existing account with same server and username
            val existingAccount =
                secureAuthStorage.observeAccounts().value
                    .firstOrNull { it.serverUrl == normalizedServer && it.username == trimmedUsername }
            
            if (existingAccount != null) {
                // Update existing account instead of creating duplicate
                val service = clientFactory.createWithBasicAuth(normalizedServer, trimmedUsername, trimmedPassword)
                val user = runCatching { service.fetchUser() }.getOrElse { throw mapError(it) }
                val updatedAccount =
                    existingAccount.copy(
                        displayName = user.body.data.displayName ?: trimmedUsername,
                        authType = AuthType.PASSWORD.name,
                        appPassword = trimmedPassword,
                        accessToken = null,
                        refreshToken = null,
                    )
                secureAuthStorage.saveAccount(updatedAccount)
                secureAuthStorage.setActiveAccount(updatedAccount.id)
                return updatedAccount.toDomain()
            }

            val service = clientFactory.createWithBasicAuth(normalizedServer, trimmedUsername, trimmedPassword)
            val user = runCatching { service.fetchUser() }.getOrElse { throw mapError(it) }
            val account =
                StoredAccount(
                    id = UUID.randomUUID().toString(),
                    serverUrl = normalizedServer,
                    username = trimmedUsername,
                    displayName = user.body.data.displayName ?: trimmedUsername,
                    authType = AuthType.PASSWORD.name,
                    appPassword = trimmedPassword,
                )
            secureAuthStorage.saveAccount(account)
            secureAuthStorage.setActiveAccount(account.id)
            return account.toDomain()
        }

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
            val username =
                user.body.data.id
                    .orEmpty()
            
            // Check for existing account with same server and username
            val existingAccount =
                secureAuthStorage.observeAccounts().value
                    .firstOrNull { it.serverUrl == normalizedServer && it.username == username }
            
            if (existingAccount != null) {
                // Update existing account instead of creating duplicate
                val updatedAccount =
                    existingAccount.copy(
                        displayName = user.body.data.displayName ?: user.body.data.email ?: username,
                        authType = AuthType.OAUTH.name,
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                        appPassword = null,
                    )
                secureAuthStorage.saveAccount(updatedAccount)
                secureAuthStorage.setActiveAccount(updatedAccount.id)
                return updatedAccount.toDomain()
            }
            
            val account =
                StoredAccount(
                    id = UUID.randomUUID().toString(),
                    serverUrl = normalizedServer,
                    username = username,
                    displayName = user.body.data.displayName ?: user.body.data.email ?: username,
                    authType = AuthType.OAUTH.name,
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                )
            secureAuthStorage.saveAccount(account)
            secureAuthStorage.setActiveAccount(account.id)
            return account.toDomain()
        }

        override fun observeAccounts(): Flow<List<NextcloudAccount>> =
            secureAuthStorage.observeAccounts().map { stored -> stored.map { it.toDomain() } }

        override fun observeActiveAccount(): Flow<NextcloudAccount?> =
            combine(secureAuthStorage.observeAccounts(), secureAuthStorage.observeActiveAccountId()) { accounts, activeId ->
                accounts.firstOrNull { it.id == activeId }?.toDomain()
            }

        override suspend fun switchAccount(accountId: String) {
            if (secureAuthStorage.findAccount(accountId) == null) {
                throw AuthFailure.AccountNotFound
            }
            secureAuthStorage.setActiveAccount(accountId)
        }

        override suspend fun logout(accountId: String) {
            secureAuthStorage.removeAccount(accountId)
        }

        private fun normalizeOrThrow(serverUrl: String): String =
            when (val validation = validateServerUrlUseCase(serverUrl)) {
                is ValidationResult.Invalid -> throw AuthFailure.InvalidServerUrl(validation.error)
                is ValidationResult.Valid -> validation.normalizedUrl
            }

        private fun mapError(throwable: Throwable): AuthFailure =
            when (throwable) {
                is HttpException ->
                    if (throwable.code() == 401) {
                        AuthFailure.InvalidCredentials
                    } else {
                        AuthFailure.Network.ServerError(throwable.code())
                    }

                is SecurityException ->
                    AuthFailure.Network.PermissionDenied

                is UnknownHostException -> AuthFailure.Network.Unreachable
                is SSLPeerUnverifiedException, is SSLHandshakeException ->
                    AuthFailure.CertificateError
                else -> AuthFailure.Unexpected(throwable)
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
