package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.Account
import com.nextcloud.tasks.domain.model.AuthType
import com.nextcloud.tasks.domain.repository.AccountRepository
import java.util.UUID

class PerformOAuthLoginUseCase(
    private val repository: AccountRepository,
    private val validateServerUrlUseCase: ValidateServerUrlUseCase,
) {
    suspend operator fun invoke(
        serverUrl: String,
        accessToken: String,
        refreshToken: String?,
        displayName: String,
    ): Result<Account> {
        if (accessToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Access token is required"))
        }
        if (displayName.isBlank()) {
            return Result.failure(IllegalArgumentException("Display name is required"))
        }

        val validatedServer = validateServerUrlUseCase(serverUrl).getOrElse { return Result.failure(it) }
        val account =
            Account(
                id = UUID.randomUUID().toString(),
                serverUrl = validatedServer.normalizedUrl,
                displayName = displayName.trim(),
                username = null,
                authType = AuthType.OAUTH,
                accessToken = accessToken.trim(),
                refreshToken = refreshToken?.takeIf { it.isNotBlank() },
                appPassword = null,
            )

        repository.saveAccount(account)
        repository.setActiveAccount(account.id)
        return Result.success(account)
    }
}
