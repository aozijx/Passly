package com.aozijx.passly.security.authentication

import android.content.Context
import androidx.biometric.BiometricManager
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.ErrorCode
import com.aozijx.passly.core.telemetry.SafeLogValue
import com.aozijx.passly.domain.auth.model.envelope.EnvelopeType
import com.aozijx.passly.domain.authentication.AuthMethodAvailability
import com.aozijx.passly.domain.authentication.AuthenticationCallback
import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationRequestHandle
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.AuthenticationSnapshot
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.LockReason
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import com.aozijx.passly.security.envelope.BootstrapStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthenticationManager @Inject constructor(
    @ApplicationContext context: Context,
    private val scope: CoroutineScope,
    private val hostRegistry: AuthenticationHostRegistry,
    private val bootstrapStore: BootstrapStore,
    private val biometricExecutor: BiometricMethodExecutor,
    private val credentialExecutor: CredentialMethodExecutor,
    private val session: VaultSessionController,
    private val settingsRepository: AppSettingsRepository
) : AuthenticationManager {
    private val biometricManager = BiometricManager.from(context)
    private val requestMutex = Mutex()
    private val activeCorrelationId = AtomicReference<String?>(null)
    private val _methods = MutableStateFlow(AuthMethodAvailability())

    /** 记录最近一次成功的认证方式，用于会话复用场景。 */
    @Volatile
    private var lastAuthMethod: AuthenticationMethod = AuthenticationMethod.BIOMETRIC

    override val state: StateFlow<AuthenticationState> = session.authenticationState
    override val methods: StateFlow<AuthMethodAvailability> = _methods
    override val databaseFailure: StateFlow<Throwable?> = session.databaseFailure

    override suspend fun completeDatabaseRecovery(): Boolean =
        session.completeDatabaseRecovery()

    override fun clearDatabaseFailure() {
        session.clearDatabaseFailure()
    }

    init {
        scope.launch { refreshAvailability() }
    }

    // ============================== 认证策略（Policy 内联实现） ==============================

    /**
     * 指定目的是否需要新鲜认证。
     *
     * 高敏感字段的显示、备份和安全管理操作始终要求新鲜认证。
     * 复制请求是否重新认证只由全局复制验证开关控制。
     */
    private suspend fun requiresFreshAuthentication(purpose: AuthenticationPurpose): Boolean =
        authenticationRequiresFresh(
            purpose = purpose,
            reauthenticateSensitiveCopies = if (purpose == AuthenticationPurpose.COPY_SECRET) {
                settingsRepository.settings.first()
                    .security.reauthenticateSensitiveCopies
            } else {
                true
            }
        )

    /**
     * 指定目的允许的认证方式。
     *
     * - RECOVERY_CODE 仅限 UNLOCK_VAULT
     * - RECOVER_DATABASE / CLEAR_DATABASE 允许恢复码，确保数据库不可用时仍有恢复路径
     * - 其他非解锁目的仅限 BIOMETRIC 和 APP_PASSWORD
     */
    private fun allowedMethods(purpose: AuthenticationPurpose): Set<AuthenticationMethod> =
        when (purpose) {
            AuthenticationPurpose.UNLOCK_VAULT,
            AuthenticationPurpose.RECOVER_DATABASE,
            AuthenticationPurpose.CLEAR_DATABASE -> AuthenticationMethod.entries.toSet()
            else -> setOf(AuthenticationMethod.BIOMETRIC, AuthenticationMethod.APP_PASSWORD)
        }

    // ============================== 认证 ==============================

    override suspend fun authenticate(
        request: AuthenticationRequest,
        credential: CharArray?
    ): AuthenticationResult {
        if (!requestMutex.tryLock()) {
            return finish(
                request,
                AuthenticationResult.Failure(
                    AuthenticationFailure(AuthenticationFailureCode.BUSY, request.correlationId)
                )
            )
        }
        activeCorrelationId.set(request.correlationId)
        val wasUnlocked = session.isUnlocked()
        val isFullUnlock = request.purpose == AuthenticationPurpose.UNLOCK_VAULT
        try {
            // 策略决策：是否可复用会话
            if (wasUnlocked && !requiresFreshAuthentication(request.purpose)) {
                session.onUserInteraction()
                // 返回上一次认证方式，而非硬编码 BIOMETRIC
                return AuthenticationResult.Success(
                    method = lastAuthMethod,
                    reusedSession = true
                )
            }
            refreshAvailability()
            if (isFullUnlock) {
                session.transition(AuthenticationState.AwaitingHost(request.correlationId))
            }
            val lease = hostRegistry.awaitLease() ?: return finish(
                request,
                AuthenticationResult.Failure(
                    AuthenticationFailure(
                        AuthenticationFailureCode.HOST_UNAVAILABLE,
                        request.correlationId
                    )
                )
            ).also { if (isFullUnlock) restoreState(wasUnlocked) }
            // 策略决策：根据目的确定可用的认证方式。
            // 调用者的 allowedMethods 只能缩小范围，不能扩权。
            val authorizedByPurpose = allowedMethods(request.purpose)
            val available = request.allowedMethods
                .intersect(authorizedByPurpose)
                .filter(_methods.value::available)
            if (available.isEmpty()) {
                if (isFullUnlock) restoreState(wasUnlocked)
                return finish(
                    request,
                    AuthenticationResult.Failure(
                        AuthenticationFailure(
                            AuthenticationFailureCode.METHOD_UNAVAILABLE,
                            request.correlationId
                        )
                    )
                )
            }
            val host = lease.hostOrNull() ?: run {
                if (isFullUnlock) restoreState(wasUnlocked)
                return finish(
                    request,
                    AuthenticationResult.Failure(
                        AuthenticationFailure(
                            AuthenticationFailureCode.HOST_UNAVAILABLE,
                            request.correlationId
                        )
                    )
                )
            }
            val method = when {
                available.size == 1 -> available.first()
                AuthenticationMethod.BIOMETRIC in available -> AuthenticationMethod.BIOMETRIC
                else -> host.chooseMethod(request.purpose, available)
                    ?: run {
                        if (isFullUnlock) restoreState(wasUnlocked)
                        return finish(request, AuthenticationResult.Cancelled(byUser = true))
                    }
            }
            if (!hostRegistry.isCurrent(lease)) {
                if (isFullUnlock) restoreState(wasUnlocked)
                return finish(
                    request,
                    AuthenticationResult.Failure(
                        AuthenticationFailure(
                            AuthenticationFailureCode.HOST_UNAVAILABLE,
                            request.correlationId
                        )
                    )
                )
            }
            if (isFullUnlock) {
                session.transition(
                    AuthenticationState.Authenticating(
                        request.correlationId,
                        method
                    )
                )
            }
            val execution = when (method) {
                AuthenticationMethod.BIOMETRIC -> biometricExecutor.execute(request, host)
                AuthenticationMethod.APP_PASSWORD,
                AuthenticationMethod.RECOVERY_CODE -> credentialExecutor.execute(
                    request,
                    method,
                    host,
                    credential
                )
            }
            val result = when (execution) {
                is MethodExecutionResult.Success -> {
                    if (!isFullUnlock) session.onUserInteraction()
                    lastAuthMethod = execution.method
                    AuthenticationResult.Success(execution.method)
                }
                is MethodExecutionResult.Cancelled -> {
                    if (isFullUnlock) restoreState(wasUnlocked)
                    AuthenticationResult.Cancelled(execution.byUser)
                }
                is MethodExecutionResult.Failure -> {
                    if (isFullUnlock) restoreState(wasUnlocked)
                    AuthenticationResult.Failure(execution.failure)
                }
            }
            return finish(request, result)
        } catch (cancelled: CancellationException) {
            if (isFullUnlock) restoreState(wasUnlocked)
            throw cancelled
        } catch (failure: Throwable) {
            if (isFullUnlock) restoreState(wasUnlocked)
            return finish(
                request,
                AuthenticationResult.Failure(
                    AuthenticationFailure(
                        AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
                        request.correlationId
                    )
                )
            )
        } finally {
            activeCorrelationId.compareAndSet(request.correlationId, null)
            requestMutex.unlock()
        }
    }

    override fun authenticate(
        request: AuthenticationRequest,
        callback: AuthenticationCallback
    ): AuthenticationRequestHandle {
        val job = scope.launch {
            val result = authenticate(request, credential = null)
            withContext(Dispatchers.Main.immediate) { callback.onResult(result) }
        }
        return object : AuthenticationRequestHandle {
            override val correlationId: String = request.correlationId
            override fun cancel() = job.cancel()
        }
    }

    override suspend fun lock(reason: LockReason) = session.lock(reason)

    override suspend fun refreshAvailability() {
        _methods.value = withContext(Dispatchers.Default) {
            val biometricState = bootstrapStore.loadBiometricState()
            AuthMethodAvailability(
                biometric = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                ) == BiometricManager.BIOMETRIC_SUCCESS &&
                    bootstrapStore.load(EnvelopeType.BIOMETRIC) != null &&
                    biometricState.binding != null,
                appPassword = bootstrapStore.load(EnvelopeType.APP_PASSWORD) != null,
                recoveryCode = bootstrapStore.load(EnvelopeType.RECOVERY) != null
            )
        }
    }

    override fun snapshot(): AuthenticationSnapshot {
        val current = state.value
        return AuthenticationSnapshot(
            state = current,
            activeCorrelationId = activeCorrelationId.get(),
            authenticatedAtMs = (current as? AuthenticationState.Authenticated)?.authenticatedAtMs
        )
    }

    override fun onUserInteraction() = session.onUserInteraction()

    private suspend fun restoreState(wasUnlocked: Boolean) {
        if (wasUnlocked) session.markAuthenticated()
        else session.transition(AuthenticationState.Locked)
    }

    private fun finish(
        request: AuthenticationRequest,
        result: AuthenticationResult
    ): AuthenticationResult {
        if (result is AuthenticationResult.Failure) {
            AppTelemetry.w(
                EventCategory.AUTHENTICATION,
                "authentication_failed",
                fields = mapOf(
                    "code" to SafeLogValue.ErrorCodeValue(ErrorCode(result.failure.code))
                )
            )
        }
        return result
    }
}

internal fun authenticationRequiresFresh(
    purpose: AuthenticationPurpose,
    reauthenticateSensitiveCopies: Boolean
): Boolean = when (purpose) {
    AuthenticationPurpose.UNLOCK_VAULT,
    AuthenticationPurpose.REVEAL_SECRET -> false

    AuthenticationPurpose.COPY_SECRET -> reauthenticateSensitiveCopies
    AuthenticationPurpose.REVEAL_HIGH_SENSITIVITY_SECRET -> true
    else -> true
}
