package com.aozijx.passly.domain.auth.model

/**
 * 认证结果。
 *
 * 比旧的 [com.aozijx.passly.domain.authentication.AuthenticationResult] 增加了 [grant]，
 * 供调用方在后续敏感操作中验证认证仍然有效。
 */
sealed interface AuthResult {
    /** 成功，附带授权凭据 */
    data class Granted(
        val grant: AuthorizationGrant
    ) : AuthResult

    /** 用户取消 */
    data class Cancelled(val byUser: Boolean) : AuthResult

    /** 失败 */
    data class Failed(
        val code: AuthFailureCode,
        val message: String? = null
    ) : AuthResult
}

enum class AuthFailureCode {
    BUSY,
    HOST_UNAVAILABLE,
    METHOD_UNAVAILABLE,
    CREDENTIAL_INCORRECT,
    RATE_LIMITED,
    SESSION_TRANSITION_FAILED,
    GRANT_EXPIRED,
}
