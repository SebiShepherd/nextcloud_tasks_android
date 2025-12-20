package com.nextcloud.tasks.domain.model

/**
 * Error codes for authentication failures.
 * These codes should be localized in the presentation layer.
 */
enum class AuthErrorCode {
    /** Invalid credentials (username, password, or authorization code) */
    INVALID_CREDENTIALS,

    /** Server is unreachable (network error) */
    SERVER_UNREACHABLE,

    /** Server returned an error response */
    SERVER_ERROR,

    /** Network access denied (missing INTERNET permission) */
    NETWORK_ACCESS_DENIED,

    /** SSL certificate verification failed */
    CERTIFICATE_ERROR,

    /** Account not found */
    ACCOUNT_NOT_FOUND,

    /** An unexpected error occurred */
    UNEXPECTED_ERROR,
}
