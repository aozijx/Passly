package com.aozijx.passly.security.session
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.AuthorizationPermitRevoker
import com.aozijx.passly.domain.access.port.DatabaseResetSessionCoordinator
import com.aozijx.passly.domain.access.port.DatabaseSessionFailureState
import com.aozijx.passly.domain.access.port.DatabaseSessionRecovery
import com.aozijx.passly.domain.access.port.DatabaseSessionRetryResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionActivityReporter
import com.aozijx.passly.domain.access.port.SessionVisibilityController
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.security.authentication.method.credential.OwnedBytes
import com.aozijx.passly.security.autofill.inbox.AutofillInboxKeyProvisioning
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.DekUnlockResult
import com.aozijx.passly.security.dek.SensitiveDataKeyClearer
import com.aozijx.passly.security.lock.LockStateManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
internal class VaultSessionController
    @Inject
    internal constructor(
        private val dekManager: DekManager,
        private val sensitiveDataKeyClearer: SensitiveDataKeyClearer,
        private val authorizationPermitRevoker: AuthorizationPermitRevoker,
        private val sessionManager: DatabaseSessionLifecycle,
        private val vaultBootstrapStore: VaultBootstrapStore,
        private val lockStateManager: LockStateManager,
        private val securityEvents: SecuritySessionEventLogger,
        private val idleTimeoutSettings: IdleTimeoutSettings,
        private val autofillInboxKeyProvisioning: AutofillInboxKeyProvisioning,
    ) : SecureSessionAccessState,
        SessionActivityReporter,
        SessionVisibilityController,
        DatabaseSessionFailureState,
        DatabaseSessionRecovery,
        DatabaseResetSessionCoordinator {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val mutex = Mutex()
        private val mutableAuthenticationState =
            MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)
        private val mutableDatabaseFailure = MutableStateFlow<Throwable?>(null)
        private val databaseResetCoordinator =
            SecureDatabaseResetCoordinator(
                sessionMutex = mutex,
                sessionManager = sessionManager,
                lockStateManager = lockStateManager,
                authorizationPermitRevoker = authorizationPermitRevoker,
                sensitiveDataKeyClearer = sensitiveDataKeyClearer,
                authenticationState = mutableAuthenticationState,
                databaseFailure = mutableDatabaseFailure,
            )
        private var idleJob: Job? = null
        private val visibilityMonitor = Any()
        private var appInForeground = false
        private var backgroundGeneration = 0L
        private var backgroundJob: Job? = null
        private var backgroundLockStarted = false

        override val authenticationState: StateFlow<AuthenticationState> = mutableAuthenticationState.asStateFlow()
        override val databaseFailure: StateFlow<Throwable?> = mutableDatabaseFailure.asStateFlow()

        override fun isUnlocked(): Boolean = lockStateManager.isUnlocked() && sessionManager.lockState == SecureSessionState.UNLOCKED

        override fun isRecoveryMode(): Boolean = mutableAuthenticationState.value is AuthenticationState.RecoveryMode

        override fun isLocked(): Boolean = lockStateManager.isLocked() || sessionManager.lockState != SecureSessionState.UNLOCKED

        // ============================== 解锁 ==============================

        /**
         * 提交完整解锁（SEALED / SOFT_LOCKED → UNLOCKED）。
         *
         * 设置 DEK 并打开数据库。用于密码/恢复码解锁路径。
         */
        suspend fun commitUnlock(
            type: EnvelopeType,
            ownedDek: OwnedBytes,
            correlationId: String,
            purpose: AuthenticationPurpose,
        ): Boolean =
            mutex.withLock {
                val dek = ownedDek.consume()
                val previousState = mutableAuthenticationState.value
                val hadDek = dekManager.isUnlocked.value
                var sessionUnlockStarted = false
                return try {
                    transition(AuthenticationState.Unlocking(AuthenticationRequestId(correlationId)))

                    if (dekManager.isUnlocked.value) {
                        // SOFT_LOCKED: DEK 仍在内存中，凭据已验证，只需恢复数据库会话
                        sessionUnlockStarted = true
                        val err = sessionManager.unlock()
                        if (err != null) {
                            mutableDatabaseFailure.value = err
                            transition(AuthenticationState.Locked)
                            return@withLock false
                        }
                        mutableDatabaseFailure.value = null
                        lockStateManager.mark(SecureSessionState.UNLOCKED)
                    } else {
                        when (dekManager.setDek(type, dek)) {
                            DekUnlockResult.Success -> {
                                sessionUnlockStarted = true
                                val err = sessionManager.unlock()
                                if (err != null) {
                                    mutableDatabaseFailure.value = err
                                    transition(AuthenticationState.Locked)
                                    return@withLock false
                                }
                                mutableDatabaseFailure.value = null
                                lockStateManager.mark(SecureSessionState.UNLOCKED)
                            }

                            is DekUnlockResult.Failed -> {
                                transition(AuthenticationState.Locked)
                                return@withLock false
                            }
                        }
                    }

                    publishAuthenticatedInternal(purpose)
                    true
                } catch (cancelled: CancellationException) {
                    if (!hadDek && dekManager.isUnlocked.value) {
                        rollbackStagedSession(LockReason.INTEGRITY_FAILURE, LockTrigger.INTEGRITY_FAILURE)
                    } else {
                        withContext(NonCancellable) {
                            if (sessionUnlockStarted && sessionManager.lockState == SecureSessionState.UNLOCKED) {
                                try {
                                    sessionManager.softLock()
                                } catch (cleanupFailure: Throwable) {
                                    if (cleanupFailure !== cancelled) cancelled.addSuppressed(cleanupFailure)
                                }
                            }
                            lockStateManager.mark(sessionManager.lockState)
                            transition(previousState)
                        }
                    }
                    throw cancelled
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
            correlationId: String,
        ): Boolean =
            mutex.withLock {
                val dek = ownedDek.consume()
                val previousState = mutableAuthenticationState.value
                val hadDek = dekManager.isUnlocked.value
                var sessionUnlockStarted = false
                return try {
                    transition(AuthenticationState.Unlocking(AuthenticationRequestId(correlationId)))
                    if (!dekManager.isUnlocked.value) {
                        if (dekManager.setDek(EnvelopeType.RECOVERY, dek) !is DekUnlockResult.Success) {
                            transition(AuthenticationState.Locked)
                            return@withLock false
                        }
                    }
                    sessionUnlockStarted = true
                    val error = sessionManager.unlock()
                    if (error != null) {
                        mutableDatabaseFailure.value = error
                        sealStagedRecoverySession()
                        return@withLock false
                    }
                    if (!consumeRecoveryEnvelope()) return@withLock false
                    mutableDatabaseFailure.value = null
                    lockStateManager.mark(SecureSessionState.UNLOCKED)
                    markRecoveryModeInternal()
                    true
                } catch (cancelled: CancellationException) {
                    if (sessionUnlockStarted || (!hadDek && dekManager.isUnlocked.value)) {
                        rollbackStagedSession(LockReason.RECOVERY_EXIT, LockTrigger.RECOVERY_EXIT)
                    } else {
                        withContext(NonCancellable) { transition(previousState) }
                    }
                    throw cancelled
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
        suspend fun resumeSoftLock(correlationId: String = ""): Boolean =
            mutex.withLock {
                if (lockStateManager.state != SecureSessionState.SOFT_LOCKED) return@withLock false
                val err = sessionManager.unlock()
                if (err != null) {
                    mutableDatabaseFailure.value = err
                    transition(AuthenticationState.Locked)
                    return@withLock false
                }
                mutableDatabaseFailure.value = null
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
        suspend fun markAuthenticated(purpose: AuthenticationPurpose): Boolean =
            mutex.withLock {
                if (lockStateManager.state == SecureSessionState.UNLOCKED) {
                    if (mutableAuthenticationState.value is AuthenticationState.RecoveryMode) {
                        // Recovery mode must not be promoted by a generic session marker.
                        // Leaving recovery after rebuilding a primary method is handled by
                        // the recovery flow with a sealed lock and a normal re-unlock.
                        resetIdleTimer()
                    } else {
                        publishAuthenticatedInternal(purpose)
                    }
                    return@withLock true
                }
                // 尝试恢复软锁定；如果不是 SOFT_LOCKED 则走完整解锁路径
                if (lockStateManager.state == SecureSessionState.SOFT_LOCKED) {
                    val err = sessionManager.unlock()
                    if (err != null) {
                        mutableDatabaseFailure.value = err
                        mutableAuthenticationState.value = AuthenticationState.Locked
                        return@withLock false
                    }
                    mutableDatabaseFailure.value = null
                    lockStateManager.mark(SecureSessionState.UNLOCKED)
                }
                if (lockStateManager.state == SecureSessionState.SEALED) {
                    val err = sessionManager.unlock()
                    if (err != null) {
                        mutableDatabaseFailure.value = err
                        mutableAuthenticationState.value = AuthenticationState.Locked
                        return@withLock false
                    }
                    mutableDatabaseFailure.value = null
                    lockStateManager.mark(SecureSessionState.UNLOCKED)
                }
                publishAuthenticatedInternal(purpose)
                true
            }

        /** Restores the restricted state after a cancelled or failed attempt to leave recovery mode. */
        suspend fun markRecoveryMode(): Boolean =
            mutex.withLock {
                if (lockStateManager.state != SecureSessionState.UNLOCKED) return@withLock false
                markRecoveryModeInternal()
                true
            }

        private suspend fun markAuthenticatedInternal() =
            withContext(NonCancellable) {
                // Once the session has opened, finish publishing its state and provisioning without leaving a partial unlock.
                withContext(Dispatchers.Main.immediate) {
                    mutableAuthenticationState.value = AuthenticationState.Authenticated(System.currentTimeMillis())
                    resetIdleTimer()
                }
                autofillInboxKeyProvisioning.provisionAfterUnlock()
            }

        private suspend fun publishAuthenticatedInternal(purpose: AuthenticationPurpose) =
            withContext(NonCancellable) {
                withContext(Dispatchers.Main.immediate) {
                    if (purpose != AuthenticationPurpose.AUTOFILL ||
                        mutableAuthenticationState.value !is AuthenticationState.Authenticated
                    ) {
                        mutableAuthenticationState.value = authenticatedStateFor(purpose, System.currentTimeMillis())
                    }
                    resetIdleTimer()
                }
                if (purpose == AuthenticationPurpose.UNLOCK_VAULT) {
                    autofillInboxKeyProvisioning.provisionAfterUnlock()
                }
            }

        private suspend fun markRecoveryModeInternal() =
            withContext(Dispatchers.Main.immediate) {
                mutableAuthenticationState.value = AuthenticationState.RecoveryMode(System.currentTimeMillis())
                resetIdleTimer()
            }

        suspend fun transition(state: AuthenticationState) =
            withContext(Dispatchers.Main.immediate) {
                mutableAuthenticationState.value = state
            }

        override suspend fun retry(): DatabaseSessionRetryResult =
            mutex.withLock {
                DatabaseSessionRetryCoordinator(
                    sessionManager = sessionManager,
                    lockStateManager = lockStateManager,
                    currentFailure = { mutableDatabaseFailure.value },
                    updateFailure = { mutableDatabaseFailure.value = it },
                    publishAuthenticated = ::markAuthenticatedInternal,
                ).retry()
            }

        override suspend fun withExclusiveDatabaseReset(operation: suspend () -> Throwable?): Throwable? =
            databaseResetCoordinator.withExclusiveDatabaseReset(operation)

        private suspend fun consumeRecoveryEnvelope(): Boolean =
            consumeRecoveryCodeOrRollback(
                consume = { vaultBootstrapStore.delete(EnvelopeType.RECOVERY) },
                rollback = { sealStagedRecoverySession() },
            )

        /** Closes a session whose recovery code could not be durably consumed. */
        private suspend fun sealStagedRecoverySession() = rollbackStagedSession(LockReason.RECOVERY_EXIT, LockTrigger.RECOVERY_EXIT)

        private suspend fun rollbackStagedSession(
            reason: LockReason,
            trigger: LockTrigger,
        ) {
            var fatalForCaller: Error? = null
            try {
                withContext(NonCancellable) {
                    securityEvents.traceLock(
                        SessionLockRequest(reason, trigger),
                        timeoutMs = 5_000,
                    ) {
                        val fatal =
                            sealDatabaseForLock(
                                onCancellation = { recordFailure(it, Reasons.Database.Session.SessionSealFailed) },
                                onFailure = { recordFailure(it, Reasons.Database.Session.SessionSealFailed) },
                            )
                        dekManager.lock()
                        lockStateManager.mark(SecureSessionState.SEALED)
                        transition(AuthenticationState.Locked)
                        fatal?.let {
                            fatalForCaller = it
                            throw it
                        }
                    }
                }
            } catch (failure: Throwable) {
                fatalForCaller?.let { throw it }
                throw failure
            }
        }

        private suspend fun sealDatabaseForLock(
            onCancellation: (CancellationException) -> Unit,
            onFailure: (Throwable) -> Unit,
        ): Error? =
            try {
                sessionManager.seal().exceptionOrNull()?.also(onFailure) as? Error
            } catch (cancelled: CancellationException) {
                onCancellation(cancelled)
                null
            } catch (failure: Throwable) {
                onFailure(failure)
                failure as? Error
            }

        private fun revokeAuthorizationPermits(): Error? =
            try {
                authorizationPermitRevoker.revokeAll()
                null
            } catch (failure: Error) {
                failure
            }

        private suspend fun clearSensitiveDataKeys(): Error? =
            try {
                sensitiveDataKeyClearer.clear()
                null
            } catch (failure: Error) {
                failure
            }

        private fun combineFatal(
            primary: Error?,
            next: Error?,
        ): Error? =
            when {
                primary == null -> next
                next == null || primary === next -> primary
                else -> primary.apply { addSuppressed(next) }
            }

        // ============================== 锁定 ==============================

        /**
         * 锁定会话。
         *
         * 根据 [LockReason] 决定锁定强度。
         * 使用 [SecureSessionState] 强度比较，仅当目标强度高于当前状态时才执行。
         *
         * - SOFT_LOCKED（USER / IDLE_TIMEOUT）：阻止新租约、等待活动 lease 排空，数据库保持打开
         * - SEALED（AUTOFILL_REQUEST_FINISHED / BACKGROUND / INTEGRITY_FAILURE / APP_EXIT）：
         *   排干 + 关库 + 擦 DEK
         */
        suspend fun lock(request: SessionLockRequest) {
            var fatalForCaller: Error? = null
            try {
                securityEvents.traceLock(
                    request = request,
                    timeoutMs = if (request.reason.toLockLevel() == SecureSessionState.SEALED) 5_000 else 0,
                ) {
                    mutex.withLock {
                        val stateBeforeLock = mutableAuthenticationState.value
                        if (!shouldApplyLock(stateBeforeLock, request.reason)) return@withLock
                        // Preserve recovery-mode identity before publishing Locking.
                        val targetLevel =
                            if (stateBeforeLock is AuthenticationState.RecoveryMode) {
                                SecureSessionState.SEALED
                            } else {
                                request.reason.toLockLevel()
                            }
                        val shouldEscalate = lockStateManager.state.shouldEscalateTo(targetLevel)
                        if (shouldEscalate) {
                            transition(AuthenticationState.Locking(request.reason))
                            idleJob?.cancel()
                            idleJob = null
                        }

                        var cleanupCancellation: CancellationException? = null
                        var cleanupFatal: Error? = null
                        withContext(NonCancellable) {
                            cleanupFatal = revokeAuthorizationPermits()

                            when (targetLevel) {
                                SecureSessionState.SOFT_LOCKED -> {
                                    if (shouldEscalate) {
                                        try {
                                            sessionManager.softLock()
                                        } catch (cancelled: CancellationException) {
                                            cleanupCancellation = cancelled
                                        } catch (failure: Error) {
                                            recordFailure(failure, Reasons.Database.Session.SessionSoftLockFailed)
                                            cleanupFatal = combineFatal(cleanupFatal, failure)
                                        } catch (failure: Exception) {
                                            recordFailure(failure, Reasons.Database.Session.SessionSoftLockFailed)
                                        }
                                        lockStateManager.mark(SecureSessionState.SOFT_LOCKED)
                                    }
                                }

                                SecureSessionState.SEALED -> {
                                    Unit
                                }

                                SecureSessionState.UNLOCKED -> {
                                    Unit
                                }
                            }

                            // Drain the resource before clearing key material used by active leases.
                            var databaseSealed = false
                            if (targetLevel == SecureSessionState.SEALED || cleanupFatal != null) {
                                cleanupFatal =
                                    combineFatal(
                                        cleanupFatal,
                                        sealDatabaseForLock(
                                            onCancellation = { cleanupCancellation = it },
                                            onFailure = { recordFailure(it, Reasons.Database.Session.SessionSealFailed) },
                                        ),
                                    )
                                databaseSealed = true
                            }

                            cleanupFatal = combineFatal(cleanupFatal, clearSensitiveDataKeys())
                            if (targetLevel == SecureSessionState.SEALED || cleanupFatal != null) {
                                if (!databaseSealed) {
                                    cleanupFatal =
                                        combineFatal(
                                            cleanupFatal,
                                            sealDatabaseForLock(
                                                onCancellation = { cleanupCancellation = it },
                                                onFailure = { recordFailure(it, Reasons.Database.Session.SessionSealFailed) },
                                            ),
                                        )
                                }
                                dekManager.lock()
                                lockStateManager.mark(SecureSessionState.SEALED)
                            }

                            if (shouldEscalate || targetLevel == SecureSessionState.SEALED) {
                                transition(AuthenticationState.Locked)
                            }
                            cleanupFatal?.let {
                                fatalForCaller = it
                                throw it
                            }
                            cleanupCancellation?.let { throw it }
                        }
                        currentCoroutineContext().ensureActive()
                    }
                }
            } catch (failure: Throwable) {
                fatalForCaller?.let { throw it }
                throw failure
            }
        }

        override fun onUserInteraction() {
            if (isUnlocked() && synchronized(visibilityMonitor) { appInForeground }) resetIdleTimer()
        }

        private fun resetIdleTimer() {
            idleJob?.cancel()
            if (!synchronized(visibilityMonitor) { appInForeground }) {
                idleJob = null
                return
            }
            idleJob =
                scope.launch {
                    val timeoutMs = idleTimeoutSettings.lockTimeout.first()
                    delay(timeoutMs.milliseconds)
                    idleJob = null
                    lock(SessionLockRequest(LockReason.IDLE_TIMEOUT, LockTrigger.IDLE_TIMER))
                }
        }

        override fun onAppForeground() {
            val pendingJob =
                synchronized(visibilityMonitor) {
                    appInForeground = true
                    backgroundGeneration += 1
                    if (backgroundLockStarted) null else backgroundJob.also { backgroundJob = null }
                }
            pendingJob?.cancel()
            if (isUnlocked()) resetIdleTimer()
        }

        override fun onAppBackground() {
            val previousJob: Job?
            val generation: Long
            synchronized(visibilityMonitor) {
                appInForeground = false
                backgroundGeneration += 1
                generation = backgroundGeneration
                previousJob = if (backgroundLockStarted) null else backgroundJob
                backgroundJob = null
                backgroundLockStarted = false
            }
            previousJob?.cancel()

            val job =
                scope.launch {
                    val recoveryMode = isRecoveryMode()
                    if (!recoveryMode && !idleTimeoutSettings.isLockOnBackground.first()) return@launch
                    idleJob?.cancel()
                    idleJob = null
                    val delayMs = if (recoveryMode) 0L else idleTimeoutSettings.lockTimeout.first()
                    delay(delayMs.milliseconds)
                    val shouldLock =
                        synchronized(visibilityMonitor) {
                            if (appInForeground || generation != backgroundGeneration) {
                                false
                            } else {
                                backgroundLockStarted = true
                                true
                            }
                        }
                    if (!shouldLock) return@launch

                    try {
                        withContext(NonCancellable) {
                            lock(SessionLockRequest(LockReason.BACKGROUND, LockTrigger.APP_BACKGROUND))
                        }
                    } finally {
                        synchronized(visibilityMonitor) {
                            if (generation == backgroundGeneration) {
                                backgroundJob = null
                                backgroundLockStarted = false
                            }
                        }
                    }
                }
            synchronized(visibilityMonitor) {
                if (generation == backgroundGeneration && !appInForeground) {
                    backgroundJob = job
                } else {
                    job.cancel()
                }
            }
        }

        private fun LockReason.toLockLevel(): SecureSessionState =
            when (this) {
                LockReason.USER,
                LockReason.IDLE_TIMEOUT,
                -> SecureSessionState.SOFT_LOCKED

                // autofill 场景临时解锁的会话必须在填充结束后彻底回收
                // （关库 + 擦 DEK），避免解锁泄漏到下一个请求。
                LockReason.AUTOFILL_REQUEST_FINISHED,
                LockReason.BACKGROUND,
                LockReason.RECOVERY_EXIT,
                LockReason.INTEGRITY_FAILURE,
                LockReason.APP_EXIT,
                -> SecureSessionState.SEALED
            }
    }

internal fun authenticatedStateFor(
    purpose: AuthenticationPurpose,
    authenticatedAtMs: Long,
): AuthenticationState =
    when (purpose) {
        AuthenticationPurpose.AUTOFILL -> {
            AuthenticationState.AutofillAuthenticated(authenticatedAtMs)
        }

        AuthenticationPurpose.UNLOCK_VAULT -> {
            AuthenticationState.Authenticated(authenticatedAtMs)
        }

        else -> {
            error("Purpose $purpose does not open a normal Vault session")
        }
    }

internal fun shouldApplyLock(
    state: AuthenticationState,
    reason: LockReason,
): Boolean =
    reason != LockReason.AUTOFILL_REQUEST_FINISHED ||
        state !is AuthenticationState.Authenticated
