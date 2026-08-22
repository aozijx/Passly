package com.aozijx.passly.security.authentication

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.telemetry.ErrorCode
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.SafeLogValue
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.policy.AuthenticationMethodPolicy
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import com.aozijx.passly.security.dek.SensitiveDataKeyManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthenticationManager @Inject constructor(
    private val hostRegistry: AuthenticationHostRegistry,
    private val biometricExecutor: BiometricMethodExecutor,
    private val credentialExecutor: CredentialMethodExecutor,
    private val session: VaultSessionController,
    private val settingsRepository: AppSettingsRepository,
    private val availabilityResolver: AuthenticationAvailabilityResolver,
    private val sensitiveDataKeyManager: SensitiveDataKeyManager
) : AuthenticationManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val requestMutex = Mutex()
    private val _methods = MutableStateFlow(AuthenticationMethods())

    /** 记录最近一次成功的认证方式，用于会话复用场景。 */
    @Volatile
    private var lastAuthMethod: AuthenticationMethod = AuthenticationMethod.BIOMETRIC

    override val state: StateFlow<AuthenticationState> = session.authenticationState
    override val methods: StateFlow<AuthenticationMethods> = _methods

    override suspend fun completeDatabaseRecovery(): Boolean =
        session.completeDatabaseRecovery()

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
        AuthenticationMethodPolicy.requiresFreshAuthentication(
            purpose = purpose,
            reauthenticateSensitiveCopies = if (purpose == AuthenticationPurpose.COPY_SECRET) {
                settingsRepository.settings.first()
                    .security.reauthenticateSensitiveCopies
            } else {
                true
            }
        )

    // ============================== 认证 ==============================

    override suspend fun authenticate(
        request: AuthenticationRequest,
        input: AuthInput,
    ): AuthenticationResult {
        if (!requestMutex.tryLock()) {
            return finish(
                request,
                AuthenticationResult.Failure(
                    AuthenticationFailure(AuthenticationFailureCode.BUSY, request.id)
                )
            )
        }
        val previousState = session.authenticationState.value
        val wasUnlocked = session.isUnlocked()
        val wasRecoveryMode = session.isRecoveryMode()
        val opensSession = request.purpose == AuthenticationPurpose.UNLOCK_VAULT ||
            request.purpose == AuthenticationPurpose.RECOVER_AUTH_METHODS ||
            // 自动填充需要临时解锁 vault 才能检索并解密候选凭据；
            // AutofillRequestSession 会在请求结束后以 SOFT_LOCKED 收回解锁。
            request.purpose == AuthenticationPurpose.AUTOFILL
        try {
            if (wasRecoveryMode && request.purpose !in AuthenticationMethodPolicy.RECOVERY_MODE_PURPOSES) {
                return finish(
                    request,
                    AuthenticationResult.Failure(
                        AuthenticationFailure(
                            AuthenticationFailureCode.SESSION_MODE_RESTRICTED,
                            request.id
                        )
                    )
                )
            }
            // A recovery-code verification has already established this restricted session.
            // Allow only its narrowly scoped actions to reuse it.
            if (wasRecoveryMode && request.purpose in AuthenticationMethodPolicy.RECOVERY_MODE_REUSABLE_PURPOSES) {
                session.onUserInteraction()
                return AuthenticationResult.Success(
                    method = AuthenticationMethod.RECOVERY_CODE,
                    reusedSession = true
                )
            }
            // 策略决策：是否可复用会话
            if (wasUnlocked && !wasRecoveryMode &&
                !requiresFreshAuthentication(request.purpose)
            ) {
                session.onUserInteraction()
                // 返回上一次认证方式，而非硬编码 BIOMETRIC
                return AuthenticationResult.Success(
                    method = lastAuthMethod,
                    reusedSession = true
                )
            }
            refreshAvailability()
            if (opensSession) {
                session.transition(AuthenticationState.AwaitingHost(request.id))
            }
            val lease = hostRegistry.awaitLease() ?: return finish(
                request,
                AuthenticationResult.Failure(
                    AuthenticationFailure(
                        AuthenticationFailureCode.HOST_UNAVAILABLE,
                        request.id
                    )
                )
            ).also { if (opensSession) restoreState(previousState) }
            // 策略决策：根据目的确定可用的认证方式。
            // 调用者的 allowedMethods 只能缩小范围，不能扩权。
            val authorizedByPurpose = AuthenticationMethodPolicy.allowedAuthenticationMethods(request.purpose)
            val available = request.allowedMethods
                .intersect(authorizedByPurpose)
                .filter { it in _methods.value }
            if (available.isEmpty()) {
                if (opensSession) restoreState(previousState)
                return finish(
                    request,
                    AuthenticationResult.Failure(
                        AuthenticationFailure(
                            AuthenticationFailureCode.METHOD_UNAVAILABLE,
                            request.id
                        )
                    )
                )
            }
            val host = lease.hostOrNull() ?: run {
                if (opensSession) restoreState(previousState)
                return finish(
                    request,
                    AuthenticationResult.Failure(
                        AuthenticationFailure(
                            AuthenticationFailureCode.HOST_UNAVAILABLE,
                            request.id
                        )
                    )
                )
            }
            val method = when {
                available.size == 1 -> available.first()
                AuthenticationMethod.BIOMETRIC in available -> AuthenticationMethod.BIOMETRIC
                else -> host.chooseMethod(request.purpose, available)
                    ?: run {
                        if (opensSession) restoreState(previousState)
                        return finish(request, AuthenticationResult.Cancelled(CancellationReason.USER))
                    }
            }
            if (!hostRegistry.isCurrent(lease)) {
                if (opensSession) restoreState(previousState)
                return finish(
                    request,
                    AuthenticationResult.Failure(
                        AuthenticationFailure(
                            AuthenticationFailureCode.HOST_UNAVAILABLE,
                            request.id
                        )
                    )
                )
            }
            if (opensSession) {
                session.transition(
                    AuthenticationState.Authenticating(
                        request.id,
                        method
                    )
                )
            }
            val credential = input.consumeCredential()
            val execution = try {
                when (method) {
                    AuthenticationMethod.BIOMETRIC -> biometricExecutor.execute(request, host)
                    AuthenticationMethod.APP_PASSWORD,
                    AuthenticationMethod.RECOVERY_CODE -> credentialExecutor.execute(
                        request,
                        method,
                        host,
                        credential,
                    )
                }
            } finally {
                credential?.fill('\u0000')
            }
            val result = when (execution) {
                is MethodExecutionResult.Success -> {
                    if (execution.method == AuthenticationMethod.RECOVERY_CODE) {
                        // Successful recovery authentication durably consumes its envelope.
                        _methods.value = AuthenticationMethods(
                            _methods.value.available - AuthenticationMethod.RECOVERY_CODE
                        )
                    }
                    if (request.purpose.unlocksSensitiveDataKey()) {
                        sensitiveDataKeyManager.unlockAfterFreshAuthentication()
                    }
                    if (!opensSession) session.onUserInteraction()
                    lastAuthMethod = execution.method
                    AuthenticationResult.Success(execution.method)
                }
                is MethodExecutionResult.Cancelled -> {
                    if (opensSession) restoreState(previousState)
                    AuthenticationResult.Cancelled(
                        if (execution.byUser) CancellationReason.USER else CancellationReason.CALLER
                    )
                }
                is MethodExecutionResult.Failure -> {
                    if (opensSession) restoreState(previousState)
                    AuthenticationResult.Failure(execution.failure)
                }
            }
            return finish(request, result)
        } catch (cancelled: CancellationException) {
            if (opensSession) restoreState(previousState)
            throw cancelled
        } catch (failure: Throwable) {
            if (opensSession) restoreState(previousState)
            return finish(
                request,
                AuthenticationResult.Failure(
                    AuthenticationFailure(
                        AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
                        request.id
                    )
                )
            )
        } finally {
            requestMutex.unlock()
        }
    }

    override suspend fun lock(reason: LockReason) = session.lock(reason)

    override suspend fun refreshAvailability() {
        _methods.value = availabilityResolver.resolve()
    }

    override fun snapshot(): AuthenticationSnapshot {
        val current = state.value
        return AuthenticationSnapshot(
            state = current,
            methods = methods.value,
        )
    }

    private suspend fun restoreState(previousState: AuthenticationState) {
        when (previousState) {
            is AuthenticationState.Authenticated -> session.markAuthenticated()
            is AuthenticationState.RecoveryMode -> session.markRecoveryMode()
            else -> session.transition(AuthenticationState.Locked)
        }
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
                    "code" to SafeLogValue.ErrorCodeValue(ErrorCode(result.failure.code.name))
                )
            )
        }
        return result
    }

    private fun AuthenticationPurpose.unlocksSensitiveDataKey(): Boolean =
        this == AuthenticationPurpose.REVEAL_HIGH_SENSITIVITY_SECRET ||
            this == AuthenticationPurpose.BACKUP_EXPORT
}
