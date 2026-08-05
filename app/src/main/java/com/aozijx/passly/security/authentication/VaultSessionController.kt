package com.aozijx.passly.security.authentication

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.session.LockState
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.domain.auth.model.VaultLockState
import com.aozijx.passly.domain.auth.model.envelope.EnvelopeType
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.LockReason
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.UnlockResult
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
    private val sessionManager: UnifiedSessionManager,
    idleTimeoutSettings: com.aozijx.passly.domain.settings.repository.IdleTimeoutSettings
) : VaultAccessState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val _state = MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)
    private val _databaseFailure = MutableStateFlow<Throwable?>(null)
    private var idleJob: Job? = null
    private var timeoutMs = 30_000L

    /** 内部锁强度追踪，解决 SoftLocked 无法可靠升级为 Sealed 的问题 */
    @Volatile
    private var lockLevel: VaultLockState = VaultLockState.SEALED

    override val authenticationState: StateFlow<AuthenticationState> = _state.asStateFlow()
    val databaseFailure: StateFlow<Throwable?> = _databaseFailure.asStateFlow()

    override fun isUnlocked(): Boolean = lockLevel == VaultLockState.UNLOCKED
    override fun isRecoveryMode(): Boolean = _state.value is AuthenticationState.RecoveryMode
    override fun isLocked(): Boolean = lockLevel != VaultLockState.UNLOCKED

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
            transition(AuthenticationState.Unlocking(correlationId))

            if (dekManager.isUnlocked.value) {
                // SOFT_LOCKED: DEK 仍在内存中，凭据已验证，只需恢复数据库会话
                val err = sessionManager.unlock()
                if (err != null) {
                    _databaseFailure.value = err
                    transition(AuthenticationState.Locked)
                    return@withLock false
                }
                _databaseFailure.value = null
                lockLevel = VaultLockState.UNLOCKED
            } else {
                when (dekManager.setDek(type, dek)) {
                    UnlockResult.Success -> {
                        val err = sessionManager.unlock()
                        if (err != null) {
                            _databaseFailure.value = err
                            transition(AuthenticationState.Locked)
                            return@withLock false
                        }
                        _databaseFailure.value = null
                        lockLevel = VaultLockState.UNLOCKED
                    }

                    is UnlockResult.Failed -> {
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
            transition(AuthenticationState.Unlocking(correlationId))
            if (!dekManager.isUnlocked.value) {
                if (dekManager.setDek(EnvelopeType.RECOVERY, dek) !is UnlockResult.Success) {
                    transition(AuthenticationState.Locked)
                    return@withLock false
                }
            }
            val error = sessionManager.unlock()
            if (error != null) {
                _databaseFailure.value = error
                transition(AuthenticationState.Locked)
                return@withLock false
            }
            _databaseFailure.value = null
            lockLevel = VaultLockState.UNLOCKED
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
        if (lockLevel != VaultLockState.SOFT_LOCKED) return@withLock false
        val err = sessionManager.unlock()
        if (err != null) {
            _databaseFailure.value = err
            transition(AuthenticationState.Locked)
            return@withLock false
        }
        _databaseFailure.value = null
        lockLevel = VaultLockState.UNLOCKED
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
        if (lockLevel == VaultLockState.UNLOCKED) {
            // Recovery mode is intentionally promoted only after a primary method is rebuilt.
            if (_state.value is AuthenticationState.RecoveryMode) {
                markAuthenticatedInternal()
            } else {
                resetIdleTimer()
            }
            return@withLock true
        }
        // 尝试恢复软锁定；如果不是 SOFT_LOCKED 则走完整解锁路径
        if (lockLevel == VaultLockState.SOFT_LOCKED) {
            val err = sessionManager.unlock()
            if (err != null) {
                _databaseFailure.value = err
                _state.value = AuthenticationState.Locked
                return@withLock false
            }
            _databaseFailure.value = null
            lockLevel = VaultLockState.UNLOCKED
        }
        if (lockLevel == VaultLockState.SEALED) {
            val err = sessionManager.unlock()
            if (err != null) {
                _databaseFailure.value = err
                _state.value = AuthenticationState.Locked
                return@withLock false
            }
            _databaseFailure.value = null
            lockLevel = VaultLockState.UNLOCKED
        }
        markAuthenticatedInternal()
        true
    }

    /** Restores the restricted state after a cancelled or failed attempt to leave recovery mode. */
    suspend fun markRecoveryMode(): Boolean = mutex.withLock {
        if (lockLevel != VaultLockState.UNLOCKED) return@withLock false
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
            if (dekManager.isUnlocked.value) {
                // SOFT_LOCKED: DEK 仍在内存中，无需重新设置
                true
            } else {
                dekManager.setDek(type, dek) is UnlockResult.Success
            }
        } finally {
            dek.fill(0)
            ownedDek.discard()
        }
    }

    /**
     * 新数据库已经由恢复流程成功打开后，才发布 UNLOCKED / Authenticated。
     */
    suspend fun completeDatabaseRecovery(): Boolean = mutex.withLock {
        if (sessionManager.lockState != LockState.UNLOCKED) return@withLock false
        _databaseFailure.value = null
        lockLevel = VaultLockState.UNLOCKED
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

    // ============================== 锁定 ==============================

    /**
     * 锁定会话。
     *
     * 根据 [LockReason] 决定锁定强度。
     * 使用 [VaultLockState] 强度比较，仅当目标强度高于当前状态时才执行。
     *
     * - SOFT_LOCKED（USER / IDLE_TIMEOUT / AUTOFILL_REQUEST_FINISHED）：
     *   阻止新租约，数据库保持打开
     * - SEALED（BACKGROUND / INTEGRITY_FAILURE / APP_EXIT）：排干 + 关库 + 擦 DEK
     */
    suspend fun lock(reason: LockReason) {
        mutex.withLock {
            // A recovery session never degrades to a soft lock: closing it must wipe its DEK.
            val targetLevel = if (_state.value is AuthenticationState.RecoveryMode) {
                VaultLockState.SEALED
            } else {
                reason.toLockLevel()
            }
            if (!lockLevel.shouldEscalateTo(targetLevel)) {
                // SEALED 状态仍需确保残留 DEK 被擦除。例如数据库打开失败时，
                // lockLevel 尚未解锁，但认证执行器可能已暂存 DEK。
                if (targetLevel == VaultLockState.SEALED) {
                    runCatching { sessionManager.seal() }
                    dekManager.lock()
                    transition(AuthenticationState.Locked)
                }
                return@withLock
            }
            transition(AuthenticationState.Locking(reason))
            idleJob?.cancel()

            when (targetLevel) {
                VaultLockState.SOFT_LOCKED -> {
                    runCatching { sessionManager.softLock() }
                        .onFailure { e ->
                            AppTelemetry.e(EventCategory.DATABASE, "soft_lock_failed", throwable = e)
                        }
                    lockLevel = VaultLockState.SOFT_LOCKED
                }

                VaultLockState.SEALED -> {
                    runCatching { sessionManager.seal() }
                        .onFailure { e ->
                            AppTelemetry.e(EventCategory.DATABASE, "seal_failed", throwable = e)
                        }
                    dekManager.lock()
                    lockLevel = VaultLockState.SEALED
                }

                VaultLockState.UNLOCKED -> { /* 锁定不可能目标是 UNLOCKED */
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

    private fun LockReason.toLockLevel(): VaultLockState = when (this) {
        LockReason.USER,
        LockReason.IDLE_TIMEOUT,
        LockReason.AUTOFILL_REQUEST_FINISHED -> VaultLockState.SOFT_LOCKED

        LockReason.BACKGROUND,
        LockReason.RECOVERY_EXIT,
        LockReason.INTEGRITY_FAILURE,
        LockReason.APP_EXIT -> VaultLockState.SEALED
    }
}
