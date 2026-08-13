package com.aozijx.passly.domain.auth.model

import com.aozijx.passly.domain.auth.policy.AuthMethodType

/**
 * 针对某个 [com.aozijx.passly.domain.authentication.AuthenticationPurpose] 的不可变认证策略要求。
 *
 * 一次返回完整配置，避免分别调用多个方法读到不同配置状态。
 * 由 [com.aozijx.passly.domain.auth.policy.AuthPolicy.requirementFor] 生成。
 */
data class AuthRequirement(
    val allowedMethods: Set<AuthMethodType>,
    val freshness: AuthFreshness,
    val grantTtlMs: Long,
    val requiresUnlockedVault: Boolean
)
