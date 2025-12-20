package com.nextcloud.tasks.domain.model

/**
 * Error codes for server URL validation failures.
 * These codes should be localized in the presentation layer.
 */
enum class ValidationErrorCode {
    /** The URL field is empty */
    EMPTY_URL,

    /** The URL format is invalid */
    INVALID_FORMAT,

    /** The URL does not use HTTPS protocol */
    HTTPS_REQUIRED,
}
