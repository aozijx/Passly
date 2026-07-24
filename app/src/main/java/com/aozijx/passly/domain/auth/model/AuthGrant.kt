package com.aozijx.passly.domain.auth.model

/**
 * 授权凭据。
 *
 * 认证成功后签发，用于在敏感操作执行前验证认证仍然有效，
 * 避免 TOCTOU（Time-of-Check Time-of-Use）问题。
 *
 * 调用模式：
 * ```
 * authCoordinator.authorize(AuthPurpose.BackupExport) { grant ->
 *     backupService.export(request, grant)
 * }
 * ```
 *
 * [grant] 仅在闭包内有效，操作完成后立即失效。
 * [AuthPolicy] 控制 Grant 的有效期，超时后需重新认证。
 */
data class AuthorizationGrant(
    val purpose: AuthPurpose,
    val issuedAt: Long,
    val expiresAt: Long,
    val correlationId: String
) {
    /** Grant 是否在有效期内 */
    val isValid: Boolean
        get() = System.currentTimeMillis() < expiresAt

    /** Grant 是否为指定目的签发 */
    fun isFor(target: AuthPurpose): Boolean = purpose == target
}
