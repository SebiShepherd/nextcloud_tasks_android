package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.repository.AuthRepository

/**
 * Use case for completing login with an app password obtained from Login Flow v2.
 * This authenticates the user and creates a new account in the app.
 */
class LoginWithAppPasswordUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        serverUrl: String,
        loginName: String,
        appPassword: String,
    ): NextcloudAccount =
        repository.loginWithAppPassword(
            serverUrl = serverUrl,
            loginName = loginName,
            appPassword = appPassword,
        )
}
