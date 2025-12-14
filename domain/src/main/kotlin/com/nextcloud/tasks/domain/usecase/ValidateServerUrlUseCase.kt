package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.ValidatedServer
import java.net.URI

class ValidateServerUrlUseCase {
    operator fun invoke(rawUrl: String): Result<ValidatedServer> {
        val cleaned = rawUrl.trim()
        if (cleaned.isBlank()) {
            return Result.failure(IllegalArgumentException("Server URL is required"))
        }

        return runCatching { URI(cleaned) }
            .mapCatching { uri ->
                val scheme = uri.scheme ?: throw IllegalArgumentException("URL must include http or https")
                if (!scheme.equals("https", ignoreCase = true) && !scheme.equals("http", ignoreCase = true)) {
                    throw IllegalArgumentException("Unsupported URL scheme: $scheme")
                }
                val host = uri.host ?: throw IllegalArgumentException("URL must include a host")
                val normalized =
                    URI(scheme, uri.userInfo, host, uri.port, "/", null, null)
                        .toString()
                        .removeSuffix("/") + "/"
                ValidatedServer(normalizedUrl = normalized, isHttps = scheme.equals("https", ignoreCase = true))
            }
    }
}
