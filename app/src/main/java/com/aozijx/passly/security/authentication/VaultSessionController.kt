package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.diagnostics.LogCategory
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.LockReason
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.envelope.EnvelopeType
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
    idleTimeoutSettings: com.aozijx.passly.domain.repository.settings.IdleTimeoutSettings
) : VaultAccessState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val _state = MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)
    private var idleJob: Job? = null
    private var timeoutMs = 30_000L

    override val authenticationState: StateFlow<AuthenticationState> = _state.asStateFlow()

    init {
        scope.launch { idleTimeoutSettings.lockTimeout.collect { timeoutMs = it } }
    }

    /**
     * 提交解锁：设置 DEK 并打开数据库。
     *
     * 此方法处理两种场景：
     * 1. 全新解锁（SEALED → UNLOCKED）：需要 DEK 打开数据库
     * 2. 软锁定恢复（SOFT_LOCKED → UNLOCKED）：数据库已打开，仅恢复状态
     */
    suspend fun commitUnlock(
        type: EnvelopeType,
        ownedDek: OwnedBytes,
        correlationId: String
    ): Boolean = mutex.withLock {
        val dek = ownedDek.consume()
        return try {
            transition(AuthenticationState.Unlocking(correlationId))
            when (dekManager.setDek(type, dek)) {
                UnlockResult.Success -> {
                    // 打开数据库（SOFT_LOCKED 时不需 DEK，SEALED 时从 DekManager 获取）
                    val err = sessionManager.unlock()
                    if (err != null) {
                        transition(AuthenticationState.Locked)
                        return@withLock false
                    }
                    markAuthenticatedInternal()
                    true
                }
                is UnlockResult.Failed -> {
                    transition(AuthenticationState.Locked)
                    false
                }
            }
        } finally {
            dek.fill(0)
            ownedDek.discard()
        }
    }

    /**
     * 标记会话为已认证。
     *
     * 在确保 DEK 已正确设置后调用。
     * 若数据库已被封存（SEALED）且 DEK 不可用，返回 false 并回退到 Locked。
     *
     * @return true 如果解锁并打开数据库成功，false 否则
     */
    suspend fun markAuthenticated(): Boolean = mutex.withLock {
        val err = sessionManager.unlock()
        if (err != null) {
            _state.value = AuthenticationState.Locked
            return@withLock false
        }
        markAuthenticatedInternal()
        true
    }

    private suspend fun markAuthenticatedInternal() = withContext(Dispatchers.Main.immediate) {
        _state.value = AuthenticationState.Authenticated(System.currentTimeMillis())
        resetIdleTimer()
    }

    suspend fun transition(state: AuthenticationState) = withContext(Dispatchers.Main.immediate) {
        _state.value = state
    }

    /**
     * 锁定会话。
     *
     * 根据 [LockReason] 决定锁定策略：
     * - [LockReason.USER], [LockReason.IDLE_TIMEOUT] → softLock（阻止新租约，数据库保持打开）
     * - [LockReason.BACKGROUND], [LockReason.INTEGRITY_FAILURE], [LockReason.APP_EXIT] → seal（排干 + 关库 + 擦 DEK）
     */
    suspend fun lock(reason: LockReason) {
        mutex.withLock {
            if (_state.value == AuthenticationState.Locked) return
            transition(AuthenticationState.Locking(reason))
            idleJob?.cancel()

            when (reason) {
                LockReason.USER, LockReason.IDLE_TIMEOUT -> {
                    // 软锁定：阻止新租约，数据库保持打开，不擦 DEK
                    runCatching { sessionManager.softLock() }
                        .onFailure { e ->
                            AppLog.e(LogCategory.DATABASE, "soft_lock_failed", throwable = e)
                        }
                }

                LockReason.BACKGROUND,
                LockReason.INTEGRITY_FAILURE,
                LockReason.APP_EXIT -> {
                    // 封存：排干租约 → 关闭数据库 → 擦除 DEK
                    runCatching { sessionManager.seal() }
                        .onFailure { e ->
                            AppLog.e(LogCategory.DATABASE, "seal_failed", throwable = e)
                        }
                    dekManager.lock()
                }
            }

            transition(AuthenticationState.Locked)
        }
    }

    fun onUserInteraction() {
        if (isUnlocked()) resetIdleTimer()
    }

    override fun isUnlocked(): Boolean = _state.value is AuthenticationState.Authenticated

    private fun resetIdleTimer() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(timeoutMs)
            lock(LockReason.IDLE_TIMEOUT)
        }
    }
}
