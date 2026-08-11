package com.aozijx.passly.domain.auth.port

import com.aozijx.passly.domain.auth.model.AuthInput
import com.aozijx.passly.domain.auth.model.AuthorizationPermit
import com.aozijx.passly.domain.auth.model.AuthorizationResult
import com.aozijx.passly.domain.authentication.AuthenticationPurpose

/**
 * 敏感操作授权闸门。
 *
 * 提供 [authorize] 模式，保证调用闭包前已完成必要认证：
 * ```
 * val result = accessGate.authorize(AuthenticationPurpose.BACKUP_EXPORT) { permit ->
 *     backupService.export(request, permit)
 * }
 * when (result) {
 *     is AuthorizationResult.Allowed -> // 成功
 *     is AuthorizationResult.Denied ->  // 认证失败
 *     is AuthorizationResult.Cancelled -> // 用户取消
 * }
 * ```
 *
 * [authorize] 负责：
 * 1. 读取 [AuthPolicy] 确定策略
 * 2. 通过 [AuthMethodCatalog] 选择认证器
 * 3. 执行认证并签发内部 [AuthorizationPermit]
 * 4. 在闭包内执行敏感操作
 * 5. 操作完成后使 Permit 失效
 *
 * 实现应确保闭包执行期间会话不会被锁定。
 */
interface AuthorizationGate {

    /**
     * 授权并执行敏感操作。
     *
     * [purpose] 标识操作类型，[input] 指定凭据来源。
     *
     * @return 授权结果（成功 / 拒绝 / 取消）
     */
    suspend fun <T> authorize(
        purpose: AuthenticationPurpose,
        input: AuthInput = AuthInput.Interactive,
        block: suspend (AuthorizationPermit) -> T
    ): AuthorizationResult<T>
}
