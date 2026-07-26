package com.nextcloud.tasks.domain.model

/**
 * Typed authentication failure. Carries only stable, non-localizable data; the `:app` layer
 * maps each case to a user-facing string resource so all wording lives in the resource files.
 */
sealed class AuthFailure : Exception() {
    data class InvalidServerUrl(
        val error: ServerUrlError,
    ) : AuthFailure()

    object InvalidCredentials : AuthFailure()

    /**
     * A network / server problem. [statusCode] is the HTTP status when the server responded
     * with an error, or null when the server could not be reached at all.
     */
    data class Network(
        val statusCode: Int? = null,
    ) : AuthFailure()

    object Certificate : AuthFailure()

    object ImportNotSupported : AuthFailure()

    object Unknown : AuthFailure()
}
