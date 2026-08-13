package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationPurpose

/**
 * 认证器接口。
 *
 * 每种认证方式实现一个 [Authenticator]。
 * - [BiometricAuthenticator]
 * - [AppPasswordAuthenticator]
 * - [RecoveryCodeAuthenticator]
 *
 * 认证器仅证明身份（返回 [AuthenticatorResult]），
 * 不参与 Grant 签发。授权由 [AuthorizationGate] 负责。
 */
interface Authenticator {

    /** 本认证器支持的方式 */
    val method: AuthenticationMethod

    /**
     * 执行认证。
     *
     * @param purpose 认证目的
     * @param input 认证输入：[Interactive] 由认证器自行交互；[AppPassword]/[RecoveryCode] 已有输入值
     * @return 仅含身份验证结果，不含授权凭据
     */
    suspend fun authenticate(
        purpose: AuthenticationPurpose,
        input: AuthInput = AuthInput.Interactive
    ): AuthenticatorResult

    /**
     * 检查当前认证方式是否可用。
     */
    suspend fun isAvailable(): Boolean
}

sealed interface AuthenticatorResult {
    data class Verified(
        val method: AuthenticationMethod,
        val verifiedAtElapsedMs: Long,
    ) : AuthenticatorResult {
        init {
            require(verifiedAtElapsedMs >= 0) { "Verification time cannot be negative" }
        }
    }

    data object UserCancelled : AuthenticatorResult
    data class Rejected(val failure: AuthenticationFailure) : AuthenticatorResult
}
