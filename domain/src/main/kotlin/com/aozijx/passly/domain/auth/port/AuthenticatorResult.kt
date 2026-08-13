package com.aozijx.passly.domain.auth.port

import com.aozijx.passly.domain.auth.failure.AuthFailure
import com.aozijx.passly.domain.auth.policy.AuthMethodType

/**
 * 认证器执行结果（纯身份验证，不含 Grant/Permit）。
 *
 * 由 [Authenticator.authenticate] 返回。
 * Grant 签发由 [com.aozijx.passly.domain.auth.port.AuthorizationGate] 负责。
 */
sealed interface AuthenticatorResult {
    /** 身份验证通过 */
    data class Verified(
        val method: AuthMethodType,
        val verifiedAtElapsedMs: Long
    ) : AuthenticatorResult

    /** 用户取消 */
    data object UserCancelled : AuthenticatorResult

    /** 认证失败 */
    data class Rejected(val failure: AuthFailure) : AuthenticatorResult
}
