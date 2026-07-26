package com.nextcloud.tasks.domain.model

/**
 * Reason a server URL failed validation. The domain layer stays free of any
 * user-facing (localizable) text; the `:app` layer maps each case to a string resource.
 */
enum class ServerUrlError {
    EMPTY,
    INVALID,
    INSECURE_HTTP,
}
