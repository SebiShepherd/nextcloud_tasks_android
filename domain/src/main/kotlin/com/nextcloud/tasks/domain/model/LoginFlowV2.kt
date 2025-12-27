package com.nextcloud.tasks.domain.model

/**
 * Result of initiating the Login Flow v2.
 * Contains URLs and token needed for the browser-based authentication flow.
 */
data class LoginFlowV2Initiation(
    val pollUrl: String,
    val loginUrl: String,
    val token: String,
)

/**
 * Credentials returned after successful Login Flow v2.
 * Contains the server URL, login name, and app-specific password.
 */
data class LoginFlowV2Credentials(
    val server: String,
    val loginName: String,
    val appPassword: String,
)

/**
 * Result of polling the Login Flow v2 endpoint.
 */
sealed class LoginFlowV2PollResult {
    /**
     * Authentication is still pending (user hasn't completed login in browser yet).
     */
    data object Pending : LoginFlowV2PollResult()

    /**
     * Authentication successful, credentials received.
     */
    data class Success(
        val credentials: LoginFlowV2Credentials,
    ) : LoginFlowV2PollResult()

    /**
     * An error occurred during polling or authentication.
     */
    data class Error(
        val message: String,
    ) : LoginFlowV2PollResult()
}
