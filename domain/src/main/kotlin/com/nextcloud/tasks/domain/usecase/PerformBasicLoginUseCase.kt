package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.Account
import com.nextcloud.tasks.domain.model.AuthType
import com.nextcloud.tasks.domain.repository.AccountRepository
import java.util.UUID

class PerformBasicLoginUseCase(
    private val repository: AccountRepository,
    private val validateServerUrlUseCase: ValidateServerUrlUseCase,
) {
    suspend operator fun invoke(serverUrl: String, username: String, password: String): Result<Account> {
        if (username.isBlank()) {
            return Result.failure(IllegalArgumentException("Username is required"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password or app password is required"))
        }

        val validatedServer = validateServerUrlUseCase(serverUrl).getOrElse { return Result.failure(it) }
        val hostLabel =
            validatedServer.normalizedUrl
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
        val account =
            Account(
                id = UUID.randomUUID().toString(),
                serverUrl = validatedServer.normalizedUrl,
                displayName = "$username@$hostLabel",
                username = username.trim(),
                authType = AuthType.BASIC,
                accessToken = null,
                refreshToken = null,
                appPassword = password,
            )

        repository.saveAccount(account)
        repository.setActiveAccount(account.id)
        return Result.success(account)
    }
}
