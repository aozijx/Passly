package com.aozijx.passly.domain.auth.port

import com.aozijx.passly.domain.auth.model.AuthorizationGrant

/**
 * Vault 访问闸门。
 *
 * 提供 [authorize] 模式，在闭包内保证认证有效性，避免 TOCTOU：
 * ```
 * accessGate.authorize(AuthPurpose.BackupExport) { grant ->
 *     backupService.export(request, grant)
 * }
 * ```
 *
 * [authorize] 负责：
 * 1. 读取 [AuthPolicy] 确定策略
 * 2. 通过 [AuthMethodCatalog] 选择认证器
 * 3. 执行认证并签发 [AuthorizationGrant]
 * 4. 在闭包内执行敏感操作
 * 5. 操作完成后使 Grant 失效
 *
 * 实现应确保闭包执行期间会话不会被锁定。
 */
interface VaultAccessGate {

    /**
     * 授权并执行敏感操作。
     *
     * [purpose] 标识操作类型，[credential] 由调用方传入（如应用密码），
     * null 表示由认证器自身与用户交互获取凭据。
     *
     * @return 操作结果，失败时携带 [AuthResult.Failed]
     */
    suspend fun <T> authorize(
        purpose: com.aozijx.passly.domain.auth.model.AuthPurpose,
        credential: CharArray? = null,
        block: suspend (AuthorizationGrant) -> T
    ): T
}
