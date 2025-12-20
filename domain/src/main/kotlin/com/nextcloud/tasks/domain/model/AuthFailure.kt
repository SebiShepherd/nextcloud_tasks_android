package com.nextcloud.tasks.domain.model

sealed class AuthFailure(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data class InvalidServerUrl(
        val errorCode: ValidationErrorCode,
    ) : AuthFailure()

    object InvalidCredentials : AuthFailure()

    data class Network(
        val errorCode: AuthErrorCode,
        val details: String? = null,
    ) : AuthFailure()

    data class Certificate(
        val errorCode: AuthErrorCode,
    ) : AuthFailure()

    data class Unexpected(
        val errorCode: AuthErrorCode,
        val details: String? = null,
    ) : AuthFailure()
}
