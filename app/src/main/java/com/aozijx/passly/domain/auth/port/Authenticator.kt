package com.aozijx.passly.domain.auth.port

import com.aozijx.passly.domain.auth.model.AuthInput
import com.aozijx.passly.domain.auth.model.AuthPurpose
import com.aozijx.passly.domain.auth.policy.AuthMethodType

/**
 * 认证器接口。
 *
 * 每种认证方式实现一个 [Authenticator]。
 * - [BiometricAuthenticator]
 * - [AppPasswordAuthenticator]
 * - [RecoveryCodeAuthenticator]
 *
 * 认证器仅证明身份（返回 [AuthenticatorResult]），
 * 不参与 Grant 签发。授权由 [VaultAccessGate] 负责。
 */
interface Authenticator {

    /** 本认证器支持的方式 */
    val methodType: AuthMethodType

    /**
     * 执行认证。
     *
     * @param purpose 认证目的
     * @param input 认证输入：[Interactive] 由认证器自行交互；[AppPassword]/[RecoveryCode] 已有输入值
     * @return 仅含身份验证结果，不含授权凭据
     */
    suspend fun authenticate(
        purpose: AuthPurpose,
        input: AuthInput = AuthInput.Interactive
    ): AuthenticatorResult

    /**
     * 检查当前认证方式是否可用。
     */
    suspend fun isAvailable(): Boolean
}
