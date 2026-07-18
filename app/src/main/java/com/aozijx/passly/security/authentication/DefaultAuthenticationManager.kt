package com.aozijx.passly.security.authentication

import android.content.Context
import androidx.biometric.BiometricManager
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.diagnostics.LogCategory
import com.aozijx.passly.domain.authentication.AuthMethodAvailability
import com.aozijx.passly.domain.authentication.AuthenticationCallback
import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationRequestHandle
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.AuthenticationSnapshot
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.LockReason
import com.aozijx.passly.domain.model.envelope.EnvelopeType
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
    private val session: VaultSessionController,
    private val feedback: AuthFeedbackPresenter
) : AuthenticationManager {
    private val biometricManager = BiometricManager.from(context)
    private val requestMutex = Mutex()
    private val activeCorrelationId = AtomicReference<String?>(null)
    private val _methods = MutableStateFlow(AuthMethodAvailability())

    override val state: StateFlow<AuthenticationState> = session.authenticationState
    override val methods: StateFlow<AuthMethodAvailability> = _methods

    override suspend fun authenticate(request: AuthenticationRequest): AuthenticationResult {
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
        try {
            if (wasUnlocked && !request.requireFreshAuthentication) {
                session.onUserInteraction()
                return AuthenticationResult.Success(
                    method = AuthenticationMethod.BIOMETRIC,
                    reusedSession = true
                )
            }
            refreshMethods()
            session.transition(AuthenticationState.AwaitingHost(request.correlationId))
            val lease = hostRegistry.awaitLease() ?: return finish(
                request,
                failure(request, AuthenticationFailureCode.HOST_UNAVAILABLE)
            ).also { restoreState(wasUnlocked) }
            val available = request.allowedMethods.filter(_methods.value::available)
            if (available.isEmpty()) {
                restoreState(wasUnlocked)
                return finish(request, failure(request, AuthenticationFailureCode.METHOD_UNAVAILABLE))
            }
            val host = lease.hostOrNull() ?: run {
                restoreState(wasUnlocked)
                return finish(request, failure(request, AuthenticationFailureCode.HOST_UNAVAILABLE))
            }
            val method = if (available.size == 1) {
                available.first()
            } else {
                host.chooseMethod(request.purpose, available)
                    ?: run {
                        restoreState(wasUnlocked)
                        return finish(request, AuthenticationResult.Cancelled(byUser = true))
                    }
            }
            if (!hostRegistry.isCurrent(lease)) {
                restoreState(wasUnlocked)
                return finish(request, failure(request, AuthenticationFailureCode.HOST_UNAVAILABLE))
            }
            session.transition(AuthenticationState.Authenticating(request.correlationId, method))
            val execution = when (method) {
                AuthenticationMethod.BIOMETRIC -> biometricExecutor.execute(request, host)
                AuthenticationMethod.APP_PASSWORD,
                AuthenticationMethod.RECOVERY_CODE -> credentialExecutor.execute(request, method, host)
            }
            val result = when (execution) {
                is MethodExecutionResult.Success -> {
                    if (request.purpose != com.aozijx.passly.domain.authentication.AuthenticationPurpose.UNLOCK_VAULT) {
                        session.markAuthenticated()
                    }
                    AuthenticationResult.Success(execution.method)
                }
                is MethodExecutionResult.Cancelled -> {
                    restoreState(wasUnlocked)
                    AuthenticationResult.Cancelled(execution.byUser)
                }
                is MethodExecutionResult.Failure -> {
                    restoreState(wasUnlocked)
                    AuthenticationResult.Failure(execution.failure)
                }
            }
            return finish(request, result)
        } catch (cancelled: CancellationException) {
            restoreState(wasUnlocked)
            throw cancelled
        } catch (failure: Throwable) {
            restoreState(wasUnlocked)
            return finish(request, failure(request, AuthenticationFailureCode.SESSION_TRANSITION_FAILED))
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
            val result = authenticate(request)
            withContext(Dispatchers.Main.immediate) { callback.onResult(result) }
        }
        return object : AuthenticationRequestHandle {
            override val correlationId: String = request.correlationId
            override fun cancel() = job.cancel()
        }
    }

    override suspend fun lock(reason: LockReason) = session.lock(reason)

    override fun snapshot(): AuthenticationSnapshot {
        val current = state.value
        return AuthenticationSnapshot(
            state = current,
            activeCorrelationId = activeCorrelationId.get(),
            authenticatedAtMs = (current as? AuthenticationState.Authenticated)?.authenticatedAtMs
        )
    }

    override fun onUserInteraction() = session.onUserInteraction()

    private suspend fun refreshMethods() {
        _methods.value = withContext(Dispatchers.Default) {
            AuthMethodAvailability(
                biometric = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                ) == BiometricManager.BIOMETRIC_SUCCESS &&
                    bootstrapStore.load(EnvelopeType.BIOMETRIC) != null,
                appPassword = bootstrapStore.load(EnvelopeType.APP_PASSWORD) != null,
                recoveryCode = bootstrapStore.load(EnvelopeType.RECOVERY) != null
            )
        }
    }

    private suspend fun restoreState(wasUnlocked: Boolean) {
        if (wasUnlocked) session.markAuthenticated()
        else session.transition(AuthenticationState.Locked)
    }

    private fun failure(request: AuthenticationRequest, code: AuthenticationFailureCode) =
        AuthenticationResult.Failure(AuthenticationFailure(code, request.correlationId))

    private fun finish(
        request: AuthenticationRequest,
        result: AuthenticationResult
    ): AuthenticationResult {
        if (result is AuthenticationResult.Failure) {
            AppLog.w(
                LogCategory.AUTHENTICATION,
                "authentication_failed",
                fields = mapOf(
                    "code" to result.failure.code,
                    "correlation_id" to request.correlationId
                )
            )
        }
        feedback.present(result, request.correlationId)
        return result
    }
}
