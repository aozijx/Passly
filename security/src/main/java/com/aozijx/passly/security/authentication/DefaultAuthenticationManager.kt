package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.policy.AuthenticationMethodPolicy
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import com.aozijx.passly.security.authentication.host.HostLease
import com.aozijx.passly.security.dek.SensitiveDataKeyManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun canReuseSession(
    state: AuthenticationState,
    storageUnlocked: Boolean,
    requiresFreshAuthentication: Boolean,
    purpose: AuthenticationPurpose,
): Boolean {
    if (!storageUnlocked || requiresFreshAuthentication) return false
    return when (state) {
        is AuthenticationState.Authenticated -> true
        is AuthenticationState.AutofillAuthenticated -> purpose == AuthenticationPurpose.AUTOFILL
        else -> false
    }
}

@Singleton
class DefaultAuthenticationManager
    @Inject
    constructor(
        private val hostRegistry: AuthenticationHostRegistry,
        private val biometricExecutor: BiometricMethodExecutor,
        private val credentialExecutor: CredentialMethodExecutor,
        private val session: VaultSessionController,
        private val settingsSource: SecuritySettingsSource,
        private val availabilityResolver: AuthenticationAvailabilityResolver,
        private val sensitiveDataKeyManager: SensitiveDataKeyManager,
        private val securityEvents: SecuritySessionEventLogger,
    ) : AuthenticationManager {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val requestSlot = AuthenticationRequestSlot()
        private val authenticationMethods = AuthenticationMethodsState()

        /** 记录最近一次成功的认证方式，用于会话复用场景。 */
        @Volatile
        private var lastAuthMethod: AuthenticationMethod = AuthenticationMethod.BIOMETRIC

        override val state: StateFlow<AuthenticationState> = session.authenticationState
        override val methods: StateFlow<AuthenticationMethods> = authenticationMethods.methods

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
                reauthenticateSensitiveCopies =
                    if (purpose == AuthenticationPurpose.COPY_SECRET) {
                        settingsSource.security.first().reauthenticateSensitiveCopies
                    } else {
                        true
                    },
            )

        // ============================== 认证 ==============================

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult =
            withConsumedCredential(input) { credential ->
                securityEvents.traceAuthentication(request.purpose) {
                    authenticateInternal(request, credential, this)
                }
            }

        private suspend fun authenticateInternal(
            request: AuthenticationRequest,
            credential: CharArray?,
            traceScope: SecuritySessionEventLogger.AuthenticationTraceScope,
        ): AuthenticationResult =
            requestSlot.withSlot(
                onBusy = {
                    AuthenticationResult.Failure(
                        AuthenticationFailure(AuthenticationFailureCode.BUSY, request.id),
                    )
                },
            ) {
                authenticateWithSlot(request, credential, traceScope)
            }

        private suspend fun authenticateWithSlot(
            request: AuthenticationRequest,
            credential: CharArray?,
            traceScope: SecuritySessionEventLogger.AuthenticationTraceScope,
        ): AuthenticationResult {
            val previousState = session.authenticationState.value
            val wasUnlocked = session.isUnlocked()
            val wasRecoveryMode = session.isRecoveryMode()
            val opensSession =
                request.purpose == AuthenticationPurpose.UNLOCK_VAULT ||
                    request.purpose == AuthenticationPurpose.RECOVER_AUTH_METHODS ||
                    // 自动填充需要临时解锁 vault 才能检索并解密候选凭据；
                    // Autofill interaction lease 会在最后一个交互结束后收回解锁。
                    request.purpose == AuthenticationPurpose.AUTOFILL
            var hostLease: HostLease? = null
            try {
                if (wasRecoveryMode && request.purpose !in AuthenticationMethodPolicy.RECOVERY_MODE_PURPOSES) {
                    return AuthenticationResult.Failure(
                        AuthenticationFailure(
                            AuthenticationFailureCode.SESSION_MODE_RESTRICTED,
                            request.id,
                        ),
                    )
                }
                // A recovery-code verification has already established this restricted session.
                // Allow only its narrowly scoped actions to reuse it.
                if (wasRecoveryMode && request.purpose in AuthenticationMethodPolicy.RECOVERY_MODE_REUSABLE_PURPOSES) {
                    session.onUserInteraction()
                    return AuthenticationResult.Success(
                        method = AuthenticationMethod.RECOVERY_CODE,
                        reusedSession = true,
                    )
                }
                // 策略决策：是否可复用会话
                if (
                    canReuseSession(
                        state = previousState,
                        storageUnlocked = wasUnlocked,
                        requiresFreshAuthentication = requiresFreshAuthentication(request.purpose),
                        purpose = request.purpose,
                    )
                ) {
                    session.onUserInteraction()
                    // 返回上一次认证方式，而非硬编码 BIOMETRIC
                    return AuthenticationResult.Success(
                        method = lastAuthMethod,
                        reusedSession = true,
                    )
                }
                refreshAvailability()
                if (opensSession) {
                    session.transition(AuthenticationState.AwaitingHost(request.id))
                }
                val lease =
                    hostRegistry.awaitLease() ?: run {
                        if (opensSession) restoreState(previousState)
                        return AuthenticationResult.Failure(
                            AuthenticationFailure(
                                AuthenticationFailureCode.HOST_UNAVAILABLE,
                                request.id,
                            ),
                        )
                    }
                hostLease = lease
                // 策略决策：根据目的确定可用的认证方式。
                // 调用者的 allowedMethods 只能缩小范围，不能扩权。
                val authorizedByPurpose = AuthenticationMethodPolicy.allowedAuthenticationMethods(request.purpose)
                val available =
                    request.allowedMethods
                        .intersect(authorizedByPurpose)
                        .filter { it in authenticationMethods.value }
                if (available.isEmpty()) {
                    if (opensSession) restoreState(previousState)
                    return AuthenticationResult.Failure(
                        AuthenticationFailure(
                            AuthenticationFailureCode.METHOD_UNAVAILABLE,
                            request.id,
                        ),
                    )
                }
                val host =
                    lease.hostOrNull() ?: run {
                        if (opensSession) restoreState(previousState)
                        return AuthenticationResult.Failure(
                            AuthenticationFailure(
                                AuthenticationFailureCode.HOST_UNAVAILABLE,
                                request.id,
                            ),
                        )
                    }
                val method =
                    when {
                        available.size == 1 -> {
                            available.first()
                        }

                        AuthenticationMethod.BIOMETRIC in available -> {
                            AuthenticationMethod.BIOMETRIC
                        }

                        else -> {
                            host.chooseMethod(request.purpose, available)
                                ?: run {
                                    if (opensSession) restoreState(previousState)
                                    return AuthenticationResult.Cancelled(CancellationReason.USER)
                                }
                        }
                    }
                if (!hostRegistry.isCurrent(lease)) {
                    if (opensSession) restoreState(previousState)
                    return AuthenticationResult.Failure(
                        AuthenticationFailure(
                            AuthenticationFailureCode.HOST_UNAVAILABLE,
                            request.id,
                        ),
                    )
                }
                if (opensSession) {
                    session.transition(
                        AuthenticationState.Authenticating(
                            request.id,
                            method,
                        ),
                    )
                }
                val execution =
                    when (method) {
                        AuthenticationMethod.BIOMETRIC -> {
                            biometricExecutor.execute(request, host)
                        }

                        AuthenticationMethod.APP_PASSWORD,
                        AuthenticationMethod.RECOVERY_CODE,
                        -> {
                            credentialExecutor.execute(
                                request,
                                method,
                                host,
                                credential,
                            )
                        }
                    }
                val result =
                    when (execution) {
                        is MethodExecutionResult.Success -> {
                            if (execution.method == AuthenticationMethod.RECOVERY_CODE) {
                                // Successful recovery authentication durably consumes its envelope.
                                authenticationMethods.onRecoveryCodeConsumed()
                            }
                            if (AuthenticationMethodPolicy.unlocksSensitiveDataKey(request.purpose)) {
                                sensitiveDataKeyManager.unlockAfterFreshAuthentication()
                            }
                            if (!opensSession) session.onUserInteraction()
                            lastAuthMethod = execution.method
                            AuthenticationResult.Success(execution.method)
                        }

                        is MethodExecutionResult.Cancelled -> {
                            if (opensSession) restoreState(previousState)
                            AuthenticationResult.Cancelled(
                                if (execution.byUser) CancellationReason.USER else CancellationReason.CALLER,
                            )
                        }

                        is MethodExecutionResult.Failure -> {
                            if (opensSession) restoreState(previousState)
                            AuthenticationResult.Failure(execution.failure)
                        }
                    }
                return result
            } catch (failure: Throwable) {
                return recoverAuthenticationFailure(
                    failure = failure,
                    requestId = request.id,
                    opensSession = opensSession,
                    recordFailure = traceScope::recordFailure,
                    restoreState = { restoreState(previousState) },
                )
            } finally {
                hostLease?.release()
            }
        }

        override suspend fun lock(request: SessionLockRequest) = session.lock(request)

        override suspend fun refreshAvailability() {
            val generation = authenticationMethods.beginRefresh()
            scope
                .async {
                    authenticationMethods.refresh(
                        generation = generation,
                        resolve = availabilityResolver::resolve,
                        onFailure = securityEvents::recordAvailabilityRefreshFailure,
                    )
                }.await()
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
                is AuthenticationState.Authenticated -> {
                    session.markAuthenticated(AuthenticationPurpose.UNLOCK_VAULT)
                }

                is AuthenticationState.AutofillAuthenticated -> {
                    session.markAuthenticated(AuthenticationPurpose.AUTOFILL)
                }

                is AuthenticationState.RecoveryMode -> {
                    session.markRecoveryMode()
                }

                else -> {
                    session.transition(AuthenticationState.Locked)
                }
            }
        }
    }
