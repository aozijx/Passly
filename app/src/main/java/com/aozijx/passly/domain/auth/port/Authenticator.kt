package com.aozijx.passly.domain.auth.port

import com.aozijx.passly.domain.auth.model.AuthResult
import com.aozijx.passly.domain.auth.policy.AuthMethodType

/**
 * 认证器接口。
 *
 * 每种认证方式实现一个 [Authenticator]，像备份 Adapter 一样可插拔。
 * - [BiometricAuthenticator]
 * - [AppPasswordAuthenticator]
 * - [RecoveryCodeAuthenticator]
 *
 * 调用方（[AuthCoordinator]）通过 [AuthMethodCatalog] 获取可用认证器，
 * 由 [AuthPolicy] 决定使用哪些方式。
 */
interface Authenticator {

    /** 本认证器支持的方式 */
    val methodType: AuthMethodType

    /**
     * 执行认证。
     *
     * @param request 认证请求（目的 + correlationId）
     * @param credential 可选的外部凭据（如应用密码输入），null 表示由认证器自身获取
     * @return 认证结果
     */
    suspend fun authenticate(
        request: AuthenticationRequest,
        credential: CharArray? = null
    ): AuthResult

    /**
     * 检查当前认证方式是否可用。
     */
    suspend fun isAvailable(): Boolean
}

/**
 * 认证请求。
 *
 * 仅包含目的和 correlationId。
 * 新鲜度、允许方式和 Grant 有效期全部由 [com.aozijx.passly.domain.auth.policy.AuthPolicy] 决定。
 */
data class AuthenticationRequest(
    val purpose: com.aozijx.passly.domain.auth.model.AuthPurpose,
    val correlationId: String = java.util.UUID.randomUUID().toString()
)
