package com.nextcloud.tasks.domain.validation

import java.net.URI

/**
 * Domain validator for server URLs. Can be used by both repositories and use cases.
 */
object ServerUrlValidator {
    fun validate(rawUrl: String): ValidationResult {
        val url = rawUrl.trim()
        if (url.isEmpty()) return ValidationResult.Invalid(ValidationError.EMPTY_URL)

        val normalized = if (url.startsWith("http")) url else "https://$url"
        val parsed = runCatching { URI(normalized) }.getOrNull()
        if (parsed == null || parsed.scheme !in setOf("https", "http") || parsed.host.isNullOrBlank()) {
            return ValidationResult.Invalid(ValidationError.INVALID_FORMAT)
        }

        if (parsed.scheme != "https") {
            return ValidationResult.Invalid(ValidationError.HTTPS_REQUIRED)
        }

        return ValidationResult.Valid(normalized.trimEnd('/'))
    }
}

sealed class ValidationResult {
    data class Invalid(
        val error: ValidationError,
    ) : ValidationResult()

    data class Valid(
        val normalizedUrl: String,
    ) : ValidationResult()
}

enum class ValidationError {
    EMPTY_URL,
    INVALID_FORMAT,
    HTTPS_REQUIRED,
}
