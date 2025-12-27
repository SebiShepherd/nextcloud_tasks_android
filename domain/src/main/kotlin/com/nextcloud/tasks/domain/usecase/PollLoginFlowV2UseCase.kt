package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.LoginFlowV2PollResult
import com.nextcloud.tasks.domain.repository.AuthRepository

/**
 * Use case for polling the Login Flow v2 endpoint.
 * This repeatedly checks if the user has completed authentication in the browser.
 */
class PollLoginFlowV2UseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        pollUrl: String,
        token: String,
    ): LoginFlowV2PollResult = repository.pollLoginFlowV2(pollUrl, token)
}
