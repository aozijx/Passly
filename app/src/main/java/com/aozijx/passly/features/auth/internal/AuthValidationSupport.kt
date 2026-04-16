package com.aozijx.passly.features.auth.internal

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle

/**
 * 认证相关的校验结果。
 */
sealed interface AuthValidationResult {
    data object Valid : AuthValidationResult
    data class Invalid(val message: String) : AuthValidationResult
}

/**
 * 提供认证流程中的通用校验与辅助方法。
 */
class AuthValidationSupport {

    /**
     * 校验认证请求的上下文环境。
     */
    fun validateAuthenticationRequest(
        activity: FragmentActivity,
        title: String
    ): AuthValidationResult {
        if (activity.isFinishing || activity.isDestroyed) {
            return AuthValidationResult.Invalid("当前页面已关闭，无法进行验证")
        }

        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return AuthValidationResult.Invalid("页面尚未就绪，请稍后重试")
        }

        if (title.isBlank()) {
            return AuthValidationResult.Invalid("验证标题不能为空")
        }

        return AuthValidationResult.Valid
    }

    /**
     * 标准化锁定超时时长，确保不低于最小值。
     */
    fun normalizeLockTimeout(timeoutMs: Long): Long {
        return timeoutMs.coerceAtLeast(MIN_LOCK_TIMEOUT_MS)
    }

    /**
     * 清理并美化错误消息。
     */
    fun sanitizeMessage(message: String?): String {
        val normalized = message?.trim().orEmpty()
        return if (normalized.isNotEmpty()) normalized else DEFAULT_ERROR_MESSAGE
    }

    companion object {
        const val MIN_LOCK_TIMEOUT_MS: Long = 5_000L
        const val DEFAULT_LOCK_TIMEOUT_MS: Long = 60_000L
        private const val DEFAULT_ERROR_MESSAGE = "发生未知错误"
    }
}