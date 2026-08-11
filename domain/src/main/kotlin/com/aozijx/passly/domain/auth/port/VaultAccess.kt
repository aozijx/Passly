package com.aozijx.passly.domain.auth.port

import com.aozijx.passly.domain.auth.model.AuthInput
import com.aozijx.passly.domain.auth.model.AuthorizationResult
import com.aozijx.passly.domain.auth.model.UnlockResult
import com.aozijx.passly.domain.authentication.SecureSessionState
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.LockReason
import kotlinx.coroutines.flow.StateFlow

/**
 * Vault 访问接口（认证模块对外公开的门面）。
 *
 * 外部调用者应通过此接口访问认证功能，不应直接接触：
 * - [AuthPolicy]
 * - [AuthMethodCatalog]
 * - [Authenticator]
 * - 底层密码学细节（DEK、Envelope、Biometric Cipher）
 *
 * 职责：
 * - [state] — 暴露当前锁状态
 * - [unlock] — 解锁 Vault（从 SEALED 或 SOFT_LOCKED 到 UNLOCKED）
 * - [authorize] — 对已解锁的会话执行敏感操作授权
 * - [lock] — 锁定 Vault（可指定锁定强度）
 */
interface VaultAccess {
    val state: StateFlow<SecureSessionState>

    /**
     * 解锁 Vault。
     *
     * 内部根据当前 [SecureSessionState] 自动选择：
     * - SOFT_LOCKED → 仅恢复 Gate（不重置 DEK）
     * - SEALED → 解封 Envelope、设置 DEK、打开数据库
     */
    suspend fun unlock(input: AuthInput): UnlockResult

    /**
     * 对已解锁的会话执行授权操作。
     *
     * 如果会话已过期或已锁定，会触发重新认证。
     */
    suspend fun <T> authorize(
        purpose: AuthenticationPurpose,
        operation: suspend () -> T
    ): AuthorizationResult<T>

    /**
     * 锁定 Vault。
     *
     * 根据 [LockReason] 自动决定锁定强度：
     * - USER / IDLE_TIMEOUT → SOFT_LOCKED
     * - BACKGROUND / INTEGRITY_FAILURE / APP_EXIT → SEALED
     */
    suspend fun lock(reason: LockReason)
}
