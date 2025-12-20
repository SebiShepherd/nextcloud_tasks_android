package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.validation.ServerUrlValidator

class ValidateServerUrlUseCase {
    operator fun invoke(rawUrl: String): ValidationResult = ServerUrlValidator.validate(rawUrl)
}

// Re-export validation types for backward compatibility
typealias ValidationResult = com.nextcloud.tasks.domain.validation.ValidationResult
typealias ValidationError = com.nextcloud.tasks.domain.validation.ValidationError
