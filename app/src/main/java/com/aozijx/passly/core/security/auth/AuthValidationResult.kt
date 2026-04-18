package com.aozijx.passly.core.security.auth

/**
 * 认证相关的校验结果（Core层）。
 */
sealed interface AuthValidationResult {
    data object Valid : AuthValidationResult
    data class Invalid(val message: String) : AuthValidationResult
}