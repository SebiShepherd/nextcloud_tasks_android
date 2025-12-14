package com.nextcloud.tasks.domain.model

sealed class AuthFailure(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data class InvalidServerUrl(
        val reason: String,
    ) : AuthFailure(reason)

    object InvalidCredentials : AuthFailure("Ungültige Anmeldedaten")

    data class Network(
        val reason: String,
    ) : AuthFailure(reason)

    data class Certificate(
        val reason: String,
    ) : AuthFailure(reason)

    data class Unexpected(
        val reason: String,
    ) : AuthFailure(reason)
}
