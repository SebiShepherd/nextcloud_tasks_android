package com.nextcloud.tasks.data.auth

import com.nextcloud.tasks.domain.model.AuthType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistentAuthTokenProvider
    @Inject
    constructor(
        private val storage: SecureAuthStorage,
    ) : AuthTokenProvider {
        override fun activeToken(): AuthToken? =
            storage.activeAccount()?.let { account ->
                val authType = runCatching { AuthType.valueOf(account.authType) }.getOrNull()
                when (authType) {
                    AuthType.OAUTH ->
                        account.accessToken?.let {
                            AuthToken.OAuth(
                                serverUrl = account.serverUrl,
                                accessToken = it,
                                refreshToken = account.refreshToken,
                            )
                        }

                    AuthType.PASSWORD ->
                        account.appPassword?.let {
                            AuthToken.Password(
                                serverUrl = account.serverUrl,
                                username = account.username,
                                appPassword = it,
                            )
                        }

                    else -> null
                }
            }

        override fun activeServerUrl(): String? = storage.activeAccount()?.serverUrl
    }
