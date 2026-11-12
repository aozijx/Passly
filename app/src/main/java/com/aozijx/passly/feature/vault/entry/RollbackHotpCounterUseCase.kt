package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimePort
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext

internal class RollbackHotpCounterUseCase
    @Inject
    constructor(
        private val authorizationGate: AuthorizationGate,
        private val otpRuntime: OtpCodeRuntimePort,
    ) {
        suspend operator fun invoke(
            entryId: String,
            canRollback: () -> Boolean,
        ): Boolean {
            val callerJob = currentCoroutineContext()[Job]
            val isCurrent = { callerJob?.isActive != false && canRollback() }
            return isCurrent() &&
                try {
                    when (
                        val result =
                            authorizationGate.authorize(
                                AuthorizationScope.Global(AuthenticationPurpose.COPY_SECRET),
                            ) {
                                isCurrent() && otpRuntime.rollbackLastHotpCounter(entryId)
                            }
                    ) {
                        is AuthorizationResult.Allowed -> result.value

                        is AuthorizationResult.Denied,
                        AuthorizationResult.Cancelled,
                        -> false
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    false
                }
        }
    }
