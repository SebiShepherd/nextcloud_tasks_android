package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.LoginFlowV2Initiation
import com.nextcloud.tasks.domain.repository.AuthRepository

/**
 * Use case for initiating the Login Flow v2 process.
 * This starts the browser-based authentication flow by contacting the server
 * and obtaining the login URL and polling token.
 */
class InitiateLoginFlowV2UseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(serverUrl: String): LoginFlowV2Initiation = repository.initiateLoginFlowV2(serverUrl)
}
