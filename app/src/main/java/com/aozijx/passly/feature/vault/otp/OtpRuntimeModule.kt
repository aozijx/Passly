package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.domain.access.port.SensitiveKeyFreshnessState
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerator
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import com.aozijx.passly.domain.settings.port.OtpRuntimePolicySnapshotReader
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionStateProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

@Module
@InstallIn(SingletonComponent::class)
internal object OtpRuntimeModule {
    @Provides
    @Singleton
    fun provideOtpCodeRuntime(
        otpConfigRepository: OtpConfigRepository,
        sessionStateProvider: SessionStateProvider,
        sensitiveKeyFreshnessState: SensitiveKeyFreshnessState,
        otpRuntimePolicyReader: OtpRuntimePolicySnapshotReader,
    ): OtpCodeRuntime {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        return OtpCodeRuntime(
            scope = appScope,
            codeGenerator = { material, timestamp -> OtpGenerator.generate(material, timestamp = timestamp) },
            loadOtpMaterial = otpConfigRepository::getGenerationMaterial,
            otpRuntimePolicyReader = otpRuntimePolicyReader,
            stateDispatcher = Dispatchers.Main.immediate,
            consumeHotpCode = otpConfigRepository::consumeHotpCode,
            rollbackHotpCounter = otpConfigRepository::rollbackHotpCounter,
            initiallyUnlocked = sessionStateProvider.isWritable,
        ).apply {
            start(
                OtpRuntimeSignals(
                    unlocked = sessionStateProvider.lockStateFlow.map { it == SecureSessionState.UNLOCKED },
                    freshAuthentication = sensitiveKeyFreshnessState.generation.drop(1).map { Unit },
                ),
            )
        }
    }
}
