package com.aozijx.passly.security.authentication

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.port.AuthorizationPermitRevoker
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.SensitiveDataKeyManager
import com.aozijx.passly.security.dek.DekUnlockResult
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.security.lock.LockStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultSessionController @Inject constructor(
    private val dekManager: DekManager,
    private val sensitiveDataKeyManager: SensitiveDataKeyManager,
    private val authorizationPermitRevoker: AuthorizationPermitRevoker,
    private val sessionManager: DatabaseSessionLifecycle,
    private val vaultBootstrapStore: VaultBootstrapStore,
    private val lockStateManager: LockStateManager,
    idleTimeoutSettings: com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
) : SecureSessionAccessState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val _state = MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)
    private val _databaseFailure = MutableStateFlow<Throwable?>(null)
    private var idleJob: Job? = null
    private var timeoutMs = 30_000L

    override val authenticationState: StateFlow<AuthenticationState> = _state.asStateFlow()
    val databaseFailure: StateFlow<Throwable?> = _databaseFailure.asStateFlow()

    override fun isUnlocked(): Boolean = lockStateManager.isUnlocked()
    override fun isRecoveryMode(): Boolean = _state.value is AuthenticationState.RecoveryMode
    override fun isLocked(): Boolean = lockStateManager.isLocked()

    init {
        scope.launch { idleTimeoutSettings.lockTimeout.collect { timeoutMs = it } }
    }

    // ============================== 解锁 ==============================

    /**
     * 提交完整解锁（SEALED → UNLOCKED）。
     *
     * 设置 DEK 并打开数据库。用于密码/恢复码解锁路径。
     */
    suspend fun commitUnlock(
        type: EnvelopeType,
        ownedDek: OwnedBytes,
        correlationId: String
    ): Boolean = mutex.withLock {
        val dek = ownedDek.consume()
        return try {
            transition(AuthenticationState.Unlocking(AuthenticationRequestId(correlationId)))

            if (dekManager.isUnlocked.value) {
                // SOFT_LOCKED: DEK 仍在内存中，凭据已验证，只需恢复数据库会话
                val err = sessionManager.unlock()
                if (err != null) {
                    _databaseFailure.value = err
                    transition(AuthenticationState.Locked)
                    return@withLock false
                }
                _databaseFailure.value = null
                lockStateManager.mark(SecureSessionState.UNLOCKED)
            } else {
                when (dekManager.setDek(type, dek)) {
                    DekUnlockResult.Success -> {
                        val err = sessionManager.unlock()
                        if (err != null) {
                            _databaseFailure.value = err
                            transition(AuthenticationState.Locked)
                            return@withLock false
                        }
                        _databaseFailure.value = null
                        lockStateManager.mark(SecureSessionState.UNLOCKED)
                    }

                    is DekUnlockResult.Failed -> {
                        transition(AuthenticationState.Locked)
                        return@withLock false
                    }
                }
            }

            markAuthenticatedInternal()
            true
        } finally {
            dek.fill(0)
            ownedDek.discard()
        }
    }

    /**
     * Opens the encrypted database for the recovery workflow without granting normal Vault access.
     * Repository gates continue to reject reads because only [AuthenticationState.Authenticated]
     * represents a full session.
     */
    suspend fun commitRecoveryUnlock(
        ownedDek: OwnedBytes,
        correlationId: String
    ): Boolean = mutex.withLock {
        val dek = ownedDek.consume()
        return try {
            transition(AuthenticationState.Unlocking(AuthenticationRequestId(correlationId)))
            if (!dekManager.isUnlocked.value) {
                if (dekManager.setDek(EnvelopeType.RECOVERY, dek) !is DekUnlockResult.Success) {
                    transition(AuthenticationState.Locked)
                    return@withLock false
                }
            }
            val error = sessionManager.unlock()
            if (error != null) {
                _databaseFailure.value = error
                sealStagedRecoverySession()
                return@withLock false
            }
            if (!consumeRecoveryEnvelope()) return@withLock false
            _databaseFailure.value = null
            lockStateManager.mark(SecureSessionState.UNLOCKED)
            markRecoveryModeInternal()
            true
        } finally {
            dek.fill(0)
            ownedDek.discard()
        }
    }

    /**
     * 恢复软锁定（SOFT_LOCKED → UNLOCKED）。
     *
     * 不设置 DEK（DEK 仍然在 [DekManager] 中），只重新打开 Session Gate。
     * 用于生物识别等不需要外部凭据的重新认证。
     *
     * @return true 如果恢复成功
     */
    suspend fun resumeSoftLock(correlationId: String = ""): Boolean = mutex.withLock {
        if (lockStateManager.state != SecureSessionState.SOFT_LOCKED) return@withLock false
        val err = sessionManager.unlock()
        if (err != null) {
            _databaseFailure.value = err
            transition(AuthenticationState.Locked)
            return@withLock false
        }
        _databaseFailure.value = null
        lockStateManager.mark(SecureSessionState.UNLOCKED)
        markAuthenticatedInternal()
        true
    }

    /**
     * 标记会话为已认证。
     *
     * 用于生物识别路径。冷启动时 DEK 已由 BiometricMethodExecutor 写入，
     * 但仍必须在发布 Authenticated 前真正打开数据库。
     */
    suspend fun markAuthenticated(): Boolean = mutex.withLock {
        if (lockStateManager.state == SecureSessionState.UNLOCKED) {
            if (_state.value is AuthenticationState.RecoveryMode) {
                // Recovery mode must not be promoted by a generic session marker.
                // Leaving recovery after rebuilding a primary method is handled by
                // the recovery flow with a sealed lock and a normal re-unlock.
                resetIdleTimer()
            } else {
                resetIdleTimer()
            }
            return@withLock true
        }
        // 尝试恢复软锁定；如果不是 SOFT_LOCKED 则走完整解锁路径
        if (lockStateManager.state == SecureSessionState.SOFT_LOCKED) {
            val err = sessionManager.unlock()
            if (err != null) {
                _databaseFailure.value = err
                _state.value = AuthenticationState.Locked
                return@withLock false
            }
            _databaseFailure.value = null
            lockStateManager.mark(SecureSessionState.UNLOCKED)
        }
        if (lockStateManager.state == SecureSessionState.SEALED) {
            val err = sessionManager.unlock()
            if (err != null) {
                _databaseFailure.value = err
                _state.value = AuthenticationState.Locked
                return@withLock false
            }
            _databaseFailure.value = null
            lockStateManager.mark(SecureSessionState.UNLOCKED)
        }
        markAuthenticatedInternal()
        true
    }

    /** Restores the restricted state after a cancelled or failed attempt to leave recovery mode. */
    suspend fun markRecoveryMode(): Boolean = mutex.withLock {
        if (lockStateManager.state != SecureSessionState.UNLOCKED) return@withLock false
        markRecoveryModeInternal()
        true
    }

    /**
     * 为数据库灾难恢复暂存 DEK，但不尝试打开已知损坏的数据库。
     */
    suspend fun stageDatabaseRecovery(
        type: EnvelopeType,
        ownedDek: OwnedBytes
    ): Boolean = mutex.withLock {
        val dek = ownedDek.consume()
        return try {
            val staged = if (dekManager.isUnlocked.value) {
                // SOFT_LOCKED: DEK 仍在内存中，无需重新设置
                true
            } else {
                dekManager.setDek(type, dek) is DekUnlockResult.Success
            }
            if (!staged) return@withLock false
            if (type == EnvelopeType.RECOVERY && !consumeRecoveryEnvelope()) {
                return@withLock false
            }
            true
        } finally {
            dek.fill(0)
            ownedDek.discard()
        }
    }

    /**
     * 新数据库已经由恢复流程成功打开后，才发布 UNLOCKED / Authenticated。
     */
    suspend fun completeDatabaseRecovery(): Boolean = mutex.withLock {
        if (sessionManager.lockState != SecureSessionState.UNLOCKED) return@withLock false
        _databaseFailure.value = null
        lockStateManager.mark(SecureSessionState.UNLOCKED)
        markAuthenticatedInternal()
        true
    }

    private suspend fun markAuthenticatedInternal() = withContext(Dispatchers.Main.immediate) {
        _state.value = AuthenticationState.Authenticated(System.currentTimeMillis())
        resetIdleTimer()
    }

    private suspend fun markRecoveryModeInternal() = withContext(Dispatchers.Main.immediate) {
        _state.value = AuthenticationState.RecoveryMode(System.currentTimeMillis())
        resetIdleTimer()
    }

    suspend fun transition(state: AuthenticationState) = withContext(Dispatchers.Main.immediate) {
        _state.value = state
    }

    fun clearDatabaseFailure() {
        _databaseFailure.value = null
    }

    private suspend fun consumeRecoveryEnvelope(): Boolean =
        consumeRecoveryCodeOrRollback(
            consume = { vaultBootstrapStore.delete(EnvelopeType.RECOVERY) },
            rollback = { sealStagedRecoverySession() }
        )

    /** Closes a session whose recovery code could not be durably consumed. */
    private suspend fun sealStagedRecoverySession() {
        runCatching { sessionManager.seal() }
            .onFailure { error ->
                AppTelemetry.e(EventCategory.DATABASE, "recovery_rollback_seal_failed", throwable = error)
            }
        dekManager.lock()
        lockStateManager.mark(SecureSessionState.SEALED)
        transition(AuthenticationState.Locked)
    }

    // ============================== 锁定 ==============================

    /**
     * 锁定会话。
     *
     * 根据 [LockReason] 决定锁定强度。
     * 使用 [SecureSessionState] 强度比较，仅当目标强度高于当前状态时才执行。
     *
     * - SOFT_LOCKED（USER / IDLE_TIMEOUT）：阻止新租约，数据库保持打开
     * - SEALED（AUTOFILL_REQUEST_FINISHED / BACKGROUND / INTEGRITY_FAILURE / APP_EXIT）：
     *   排干 + 关库 + 擦 DEK
     */
    suspend fun lock(reason: LockReason) {
        mutex.withLock {
            authorizationPermitRevoker.revokeAll()
            sensitiveDataKeyManager.clear()
            // A recovery session never degrades to a soft lock: closing it must wipe its DEK.
            val targetLevel = if (_state.value is AuthenticationState.RecoveryMode) {
                SecureSessionState.SEALED
            } else {
                reason.toLockLevel()
            }
            if (!lockStateManager.state.shouldEscalateTo(targetLevel)) {
                // SEALED 状态仍需确保残留 DEK 被擦除。例如数据库打开失败时，
                // 当前锁状态尚未解锁，但认证执行器可能已暂存 DEK。
                if (targetLevel == SecureSessionState.SEALED) {
                    runCatching { sessionManager.seal() }
                    dekManager.lock()
                    transition(AuthenticationState.Locked)
                }
                return@withLock
            }
            transition(AuthenticationState.Locking(reason))
            idleJob?.cancel()

            when (targetLevel) {
                SecureSessionState.SOFT_LOCKED -> {
                    runCatching { sessionManager.softLock() }
                        .onFailure { e ->
                            AppTelemetry.e(EventCategory.DATABASE, "soft_lock_failed", throwable = e)
                        }
                    lockStateManager.mark(SecureSessionState.SOFT_LOCKED)
                }

                SecureSessionState.SEALED -> {
                    runCatching { sessionManager.seal() }
                        .onFailure { e ->
                            AppTelemetry.e(EventCategory.DATABASE, "seal_failed", throwable = e)
                        }
                    dekManager.lock()
                    lockStateManager.mark(SecureSessionState.SEALED)
                }

                SecureSessionState.UNLOCKED -> { /* 锁定不可能目标是 UNLOCKED */
                }
            }

            transition(AuthenticationState.Locked)
        }
    }

    fun onUserInteraction() {
        if (isUnlocked()) resetIdleTimer()
    }

    private fun resetIdleTimer() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(timeoutMs)
            lock(LockReason.IDLE_TIMEOUT)
        }
    }

    private fun LockReason.toLockLevel(): SecureSessionState = when (this) {
        LockReason.USER,
        LockReason.IDLE_TIMEOUT -> SecureSessionState.SOFT_LOCKED

        // autofill 场景临时解锁的会话必须在填充结束后彻底回收
        // （关库 + 擦 DEK），避免解锁泄漏到下一个请求。
        LockReason.AUTOFILL_REQUEST_FINISHED,
        LockReason.BACKGROUND,
        LockReason.RECOVERY_EXIT,
        LockReason.INTEGRITY_FAILURE,
        LockReason.APP_EXIT -> SecureSessionState.SEALED
    }
}
