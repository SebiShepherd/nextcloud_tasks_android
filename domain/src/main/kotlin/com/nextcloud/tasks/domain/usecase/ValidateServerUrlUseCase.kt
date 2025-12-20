package com.nextcloud.tasks.domain.usecase

import java.net.URI

class ValidateServerUrlUseCase {
    operator fun invoke(rawUrl: String): ValidationResult {
        val url = rawUrl.trim()
        if (url.isEmpty()) return ValidationResult.Invalid("Please enter a server URL")

        val normalized = if (url.startsWith("http")) url else "https://$url"
        val parsed = runCatching { URI(normalized) }.getOrNull()
        if (parsed == null || parsed.scheme !in setOf("https", "http") || parsed.host.isNullOrBlank()) {
            return ValidationResult.Invalid("The server URL is not valid")
        }

        if (parsed.scheme != "https") {
            return ValidationResult.Invalid("HTTPS is required for a secure connection")
        }

        return ValidationResult.Valid(parsed.toString().trimEnd('/'))
    }
}

sealed class ValidationResult {
    data class Invalid(
        val reason: String,
    ) : ValidationResult()

    data class Valid(
        val normalizedUrl: String,
    ) : ValidationResult()
}
