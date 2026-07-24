package com.aozijx.passly.domain.auth.policy

import com.aozijx.passly.domain.auth.model.AuthPurpose
import com.aozijx.passly.domain.auth.model.AuthorizationGrant

/**
 * 认证策略。
 *
 * 决定每个 [AuthPurpose] 的：
 * - 新鲜度要求（是否必须重新认证）
 * - 允许的认证方式列表
 * - Grant 有效期
 *
 * 调用者不能覆盖策略决策。这是安全的核心原则。
 */
interface AuthPolicy {

    /**
     * 指定目的是否需要新鲜认证。
     * `false` 表示可复用现有会话（仅限 UNLOCK_VAULT）。
     */
    fun requiresFreshAuthentication(purpose: AuthPurpose): Boolean

    /**
     * 指定目的允许的认证方式。
     * 返回空集表示该目的不可用。
     */
    fun allowedMethods(purpose: AuthPurpose): Set<AuthMethodType>

    /**
     * Grant 的有效期（毫秒）。
     * 超过此时间后 Grant 失效，需重新认证。
     */
    fun grantValidityMs(purpose: AuthPurpose): Long

    /**
     * 签发 Grant。
     */
    fun issueGrant(
        purpose: AuthPurpose,
        correlationId: String
    ): AuthorizationGrant
}

enum class AuthMethodType {
    BIOMETRIC,
    APP_PASSWORD,
    RECOVERY_CODE,
}
