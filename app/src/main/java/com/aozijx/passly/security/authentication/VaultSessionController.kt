package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.diagnostics.LogCategory
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.LockReason
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.authentication.VaultResourceController
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
    private val resources: VaultResourceController,
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
                    // 开启资源访问后再标记认证，确保状态监听者能正常访问数据库
                    resources.allowAccess()
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
        resources.allowAccess() // 关键修复：确保指纹等非 commitUnlock 路径也能开启数据库访问
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

            runCatching { resources.blockNewAccess() }
                .onFailure { AppLog.e(LogCategory.DATABASE, "vault_access_block_failed", throwable = it) }

            runCatching { resources.closeAndAwait() }
                .onFailure { AppLog.e(LogCategory.DATABASE, "vault_close_failed", throwable = it) }

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
