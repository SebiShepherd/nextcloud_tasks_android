package com.nextcloud.tasks.domain.usecase

import java.net.URI

class ValidateServerUrlUseCase {
    operator fun invoke(rawUrl: String): ValidationResult {
        val url = rawUrl.trim()
        val normalized = if (url.startsWith("http")) url else "https://$url"
        val parsed = runCatching { URI(normalized) }.getOrNull()
        return when {
            url.isEmpty() -> ValidationResult.Invalid("Please enter a server URL")
            parsed == null || parsed.scheme !in setOf("https", "http") || parsed.host.isNullOrBlank() ->
                ValidationResult.Invalid("The server URL is not valid")
            parsed.scheme != "https" -> ValidationResult.Invalid("HTTPS is required for a secure connection")
            else -> ValidationResult.Valid(parsed.toString().trimEnd('/'))
        }
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
