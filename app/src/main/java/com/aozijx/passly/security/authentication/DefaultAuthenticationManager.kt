package com.aozijx.passly.security.authentication

import android.content.Context
import androidx.biometric.BiometricManager
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.telemetry.EventCategory
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
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import com.aozijx.passly.security.envelope.BootstrapStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val session: VaultSessionController
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

    init {
        scope.launch { refreshAvailability() }
    }

    // ============================== 认证策略（Policy 内联实现） ==============================

    /**
     * 指定目的是否需要新鲜认证。
     *
     * 安全不变量：只有 UNLOCK_VAULT 可以在会话已解锁时复用。
     * 所有其他目的（REVEAL_SECRET、BACKUP_EXPORT、EXPORT_DIAGNOSTICS 等）
     * 必须始终触发重新认证。
     */
    private fun requiresFreshAuthentication(purpose: AuthenticationPurpose): Boolean =
        purpose != AuthenticationPurpose.UNLOCK_VAULT

    /**
     * 指定目的允许的认证方式。
     *
     * - RECOVERY_CODE 仅限 UNLOCK_VAULT
     * - 其他非解锁目的仅限 BIOMETRIC 和 APP_PASSWORD
     */
    private fun allowedMethods(purpose: AuthenticationPurpose): Set<AuthenticationMethod> =
        when (purpose) {
            AuthenticationPurpose.UNLOCK_VAULT -> AuthenticationMethod.entries.toSet()
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
                    "code" to result.failure.code,
                    "correlation_id" to request.correlationId
                )
            )
        }
        return result
    }
}
