package com.nextcloud.tasks.domain.model

import com.nextcloud.tasks.domain.validation.ValidationError

sealed class AuthFailure(
    cause: Throwable? = null,
) : Exception(cause) {
    /**
     * Server URL validation failed
     */
    data class InvalidServerUrl(
        val validationError: ValidationError,
    ) : AuthFailure()

    /**
     * Authentication credentials are invalid (HTTP 401)
     */
    object InvalidCredentials : AuthFailure()

    /**
     * Network-related errors
     */
    sealed class Network(cause: Throwable? = null) : AuthFailure(cause) {
        /**
         * Server returned an HTTP error (non-401)
         */
        data class ServerError(val httpCode: Int) : Network()

        /**
         * Network permission denied
         */
        object PermissionDenied : Network()

        /**
         * Server unreachable (DNS/connection failure)
         */
        object Unreachable : Network()
    }

    /**
     * SSL/TLS certificate verification failed
     */
    object CertificateError : AuthFailure()

    /**
     * Account not found in storage
     */
    object AccountNotFound : AuthFailure()

    /**
     * Unexpected error
     */
    data class Unexpected(
        val originalError: Throwable,
    ) : AuthFailure(originalError)
}
