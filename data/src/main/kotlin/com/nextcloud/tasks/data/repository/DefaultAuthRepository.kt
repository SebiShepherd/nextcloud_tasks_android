package com.nextcloud.tasks.data.repository

import com.nextcloud.tasks.data.auth.SecureAuthStorage
import com.nextcloud.tasks.data.auth.StoredAccount
import com.nextcloud.tasks.data.network.NextcloudClientFactory
import com.nextcloud.tasks.domain.model.AuthErrorCode
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
        private val oauthClientId = "nextcloud-tasks-android"
        private val oauthClientSecret = "local-client-secret"

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
                throw AuthFailure.Unexpected(AuthErrorCode.ACCOUNT_NOT_FOUND)
            }
            secureAuthStorage.setActiveAccount(accountId)
        }

        override suspend fun logout(accountId: String) {
            secureAuthStorage.removeAccount(accountId)
        }

        private fun normalizeOrThrow(serverUrl: String): String =
            when (val validation = validateServerUrlUseCase(serverUrl)) {
                is ValidationResult.Invalid -> throw AuthFailure.InvalidServerUrl(validation.errorCode)
                is ValidationResult.Valid -> validation.normalizedUrl
            }

        private fun mapError(throwable: Throwable): AuthFailure =
            when (throwable) {
                is HttpException ->
                    if (throwable.code() == 401) {
                        AuthFailure.InvalidCredentials
                    } else {
                        AuthFailure.Network(AuthErrorCode.SERVER_ERROR, details = throwable.code().toString())
                    }

                is SecurityException ->
                    AuthFailure.Network(AuthErrorCode.NETWORK_ACCESS_DENIED)

                is UnknownHostException -> AuthFailure.Network(AuthErrorCode.SERVER_UNREACHABLE)
                is SSLPeerUnverifiedException, is SSLHandshakeException ->
                    AuthFailure.Certificate(AuthErrorCode.CERTIFICATE_ERROR)
                else -> AuthFailure.Unexpected(AuthErrorCode.UNEXPECTED_ERROR, details = throwable.message)
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
