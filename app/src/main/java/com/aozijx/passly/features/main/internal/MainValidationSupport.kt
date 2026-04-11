package com.aozijx.passly.features.main.internal

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle

internal sealed interface MainValidationResult {
    data object Valid : MainValidationResult
    data class Invalid(val message: String) : MainValidationResult
}

internal class MainValidationSupport {

    fun validateAuthenticationRequest(
        activity: FragmentActivity,
        title: String
    ): MainValidationResult {
        if (activity.isFinishing || activity.isDestroyed) {
            return MainValidationResult.Invalid("当前页面已关闭，无法进行验证")
        }

        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return MainValidationResult.Invalid("页面尚未就绪，请稍后重试")
        }

        if (title.isBlank()) {
            return MainValidationResult.Invalid("验证标题不能为空")
        }

        return MainValidationResult.Valid
    }

    fun normalizeLockTimeout(timeoutMs: Long): Long {
        return timeoutMs.coerceAtLeast(MIN_LOCK_TIMEOUT_MS)
    }

    fun sanitizeMessage(message: String?): String {
        val normalized = message?.trim().orEmpty()
        return if (normalized.isNotEmpty()) normalized else DEFAULT_ERROR_MESSAGE
    }

    fun formatDatabaseError(error: Throwable): String {
        return "数据库错误: ${sanitizeMessage(error.message)}"
    }

    companion object {
        const val MIN_LOCK_TIMEOUT_MS: Long = 5_000L
        const val DEFAULT_LOCK_TIMEOUT_MS: Long = 60_000L
        private const val DEFAULT_ERROR_MESSAGE = "发生未知错误"
    }
}
