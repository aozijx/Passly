package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationMaterial
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerator
import com.aozijx.passly.domain.entry.otp.generation.toGenerationConfig
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import com.aozijx.passly.domain.settings.model.OtpRuntimePolicy
import com.aozijx.passly.domain.settings.port.OtpRuntimePolicySnapshotReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal fun defaultOtpRuntimePolicyReader(): OtpRuntimePolicySnapshotReader =
    object : OtpRuntimePolicySnapshotReader {
        override suspend fun readOtpRuntimePolicySnapshot() = OtpRuntimePolicy()
    }

internal fun generationMaterialForTest(config: OtpConfig): OtpGenerationMaterial =
    OtpGenerationMaterial(config.toGenerationConfig(), config.secret?.toByteArray() ?: byteArrayOf())

internal fun createTestOtpRuntime(
    repository: OtpConfigRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    otpRuntimePolicyReader: OtpRuntimePolicySnapshotReader = defaultOtpRuntimePolicyReader(),
): OtpCodeRuntime =
    OtpCodeRuntime(
        scope = scope,
        codeGenerator = { material, timestamp -> OtpGenerator.generate(material, timestamp = timestamp) },
        loadOtpMaterial = repository::getGenerationMaterial,
        otpRuntimePolicyReader = otpRuntimePolicyReader,
        stateDispatcher = Dispatchers.Unconfined,
        consumeHotpCode = repository::consumeHotpCode,
        rollbackHotpCounter = repository::rollbackHotpCounter,
    )
