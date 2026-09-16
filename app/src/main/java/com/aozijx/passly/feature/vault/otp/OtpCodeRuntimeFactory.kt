package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.domain.access.port.SensitiveKeyFreshnessState
import com.aozijx.passly.domain.entry.otp.OtpGenerator
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionStateProvider
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class OtpCodeRuntimeFactory @Inject constructor(
    private val otpConfigRepository: OtpConfigRepository,
    private val sessionStateProvider: SessionStateProvider,
    private val sensitiveKeyFreshnessState: SensitiveKeyFreshnessState,
) {
    internal fun create(scope: CoroutineScope): OtpCodeRuntime = OtpCodeRuntime(
        scope = scope,
        codeGenerator = OtpGenerator::generate,
        loadOtpConfig = otpConfigRepository::getConfig,
        initiallyUnlocked = sessionStateProvider.isWritable,
    ).also { runtime ->
        runtime.start()
        scope.launch {
            sessionStateProvider.lockStateFlow.collect { state ->
                runtime.onSessionStateChanged(state == SecureSessionState.UNLOCKED)
            }
        }
        scope.launch {
            sensitiveKeyFreshnessState.generation.drop(1).collect {
                runtime.onFreshAuthentication()
            }
        }
    }
}
