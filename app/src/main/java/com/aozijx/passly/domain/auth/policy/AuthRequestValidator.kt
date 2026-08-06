package com.aozijx.passly.domain.auth.policy

import com.aozijx.passly.domain.settings.model.LockTimeoutConstraints
import dagger.Reusable
import javax.inject.Inject

@Reusable
class AuthRequestValidator @Inject constructor() {

    sealed interface AuthRequestValidationResult {
        data object Valid : AuthRequestValidationResult
        data class Invalid(val message: String) : AuthRequestValidationResult
    }

    fun validateRequest(title: String): AuthRequestValidationResult {
        if (title.isBlank()) return AuthRequestValidationResult.Invalid("验证标题不能为空")

        return AuthRequestValidationResult.Valid
    }

    fun normalizeLockTimeout(timeoutMs: Long): Long {
        return timeoutMs.coerceAtLeast(LockTimeoutConstraints.MIN_MS)
    }

    fun sanitizeMessage(message: String?): String {
        val normalized = message?.trim().orEmpty()
        return normalized.ifEmpty { "发生未知错误" }
    }
}
