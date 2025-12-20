package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.repository.AuthRepository

class LoginWithPasswordUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        serverUrl: String,
        username: String,
        password: String,
    ): NextcloudAccount =
        repository.loginWithPassword(
            serverUrl = serverUrl,
            username = username,
            password = password,
        )
}
