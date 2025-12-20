package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.repository.AuthRepository

class LoginWithOAuthUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        serverUrl: String,
        authorizationCode: String,
        redirectUri: String,
    ): NextcloudAccount =
        repository.loginWithOAuth(
            serverUrl = serverUrl,
            authorizationCode = authorizationCode,
            redirectUri = redirectUri,
        )
}
