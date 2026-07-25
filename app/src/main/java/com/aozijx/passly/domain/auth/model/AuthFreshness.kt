package com.aozijx.passly.domain.auth.model

/**
 * 认证新鲜度要求。
 *
 * [Required] — 必须立即执行完整认证流程。
 * [SessionPermitted] — 可复用当前已验证的会话，剩余有效期为 [sessionTtlMs]（单调毫秒）。
 */
sealed interface AuthFreshness {
    data object Required : AuthFreshness
    data class SessionPermitted(val sessionTtlMs: Long) : AuthFreshness
}
