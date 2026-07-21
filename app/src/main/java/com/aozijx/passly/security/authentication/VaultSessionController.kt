package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.diagnostics.LogCategory
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.LockReason
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.envelope.EnvelopeType
import com.aozijx.passly.domain.repository.settings.IdleTimeoutSettings
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
    idleTimeoutSettings: IdleTimeoutSettings
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
                    // 打开会话（解锁状态），数据库在首次 read/write 时惰性初始化
                    sessionManager.unlock()
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
     * 必须在确保 DEK 已正确设置后调用。
     */
    suspend fun markAuthenticated() = mutex.withLock {
        sessionManager.unlock()
        markAuthenticatedInternal()
    }

    private suspend fun markAuthenticatedInternal() = withContext(Dispatchers.Main.immediate) {
        _state.value = AuthenticationState.Authenticated(System.currentTimeMillis())
        resetIdleTimer()
    }

    suspend fun transition(state: AuthenticationState) = withContext(Dispatchers.Main.immediate) {
        _state.value = state
    }

    suspend fun lock(reason: LockReason) {
        mutex.withLock {
            if (_state.value == AuthenticationState.Locked) return
            transition(AuthenticationState.Locking(reason))
            idleJob?.cancel()

            // 通知会话管理器锁定（阻塞新操作，等待活跃操作排干）
            runCatching { sessionManager.lock() }
                .onFailure { AppLog.e(LogCategory.DATABASE, "session_lock_failed", throwable = it) }

            // 擦除 DEK —— 数据库连接保持打开，但无 DEK 无法写入新数据
            dekManager.lock()
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
