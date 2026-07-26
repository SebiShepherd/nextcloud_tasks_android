package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.ServerUrlError
import java.net.URI

class ValidateServerUrlUseCase {
    operator fun invoke(rawUrl: String): ValidationResult {
        val url = rawUrl.trim()
        val normalized = if (url.startsWith("http")) url else "https://$url"
        val parsed = runCatching { URI(normalized) }.getOrNull()

        return when {
            url.isEmpty() -> ValidationResult.Invalid(ServerUrlError.EMPTY)
            parsed == null || parsed.scheme !in setOf("https", "http") || parsed.host.isNullOrBlank() ->
                ValidationResult.Invalid(ServerUrlError.INVALID)
            parsed.scheme != "https" -> ValidationResult.Invalid(ServerUrlError.INSECURE_HTTP)
            else -> ValidationResult.Valid(parsed.toString().trimEnd('/'))
        }
    }
}

sealed class ValidationResult {
    data class Invalid(
        val error: ServerUrlError,
    ) : ValidationResult()

    data class Valid(
        val normalizedUrl: String,
    ) : ValidationResult()
}
