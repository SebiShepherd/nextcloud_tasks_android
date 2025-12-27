package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.repository.AuthRepository

/**
 * @deprecated Use Login Flow v2 instead (InitiateLoginFlowV2UseCase + PollLoginFlowV2UseCase)
 */
@Deprecated("Use Login Flow v2", ReplaceWith("InitiateLoginFlowV2UseCase + PollLoginFlowV2UseCase"))
class LoginWithOAuthUseCase(
    private val repository: AuthRepository,
) {
    @Suppress("DEPRECATION")
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
