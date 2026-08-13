package com.aozijx.passly.domain.access.model

/**
 * 针对某个 [com.aozijx.passly.domain.access.model.AuthenticationPurpose] 的不可变认证策略要求。
 *
 * 一次返回完整配置，避免分别调用多个方法读到不同配置状态。
 * 由 [com.aozijx.passly.domain.access.policy.AuthenticationPolicy.requirementFor] 生成。
 */
data class AuthenticationRequirement(
    val allowedMethods: Set<AuthenticationMethod>,
    val freshness: AuthenticationFreshness,
    val grantTtlMs: Long,
    val requiresUnlockedVault: Boolean
) {
    init {
        require(grantTtlMs >= 0L) { "Authorization grant TTL cannot be negative" }
    }
}

sealed interface AuthenticationFreshness {
    data object Required : AuthenticationFreshness

    data class SessionPermitted(val ttlMs: Long) : AuthenticationFreshness {
        init {
            require(ttlMs >= 0) { "Session TTL cannot be negative" }
        }
    }
}
