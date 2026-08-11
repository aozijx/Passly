package com.aozijx.passly.domain.auth.model

import com.aozijx.passly.domain.auth.failure.AuthFailure

/**
 * 授权操作最终结果。
 *
 * 由 [com.aozijx.passly.domain.auth.port.VaultAccessGate.authorize] 返回。
 *
 * - [Allowed] — 认证通过，已签发 [AuthorizationPermit] 并在闭包内执行完成
 * - [Denied] — 认证失败，携带具体原因
 * - [Cancelled] — 用户取消
 */
sealed interface AuthorizationResult<out T> {
    data class Allowed<T>(val value: T) : AuthorizationResult<T>
    data class Denied(val failure: AuthFailure) : AuthorizationResult<Nothing>
    data object Cancelled : AuthorizationResult<Nothing>
}
