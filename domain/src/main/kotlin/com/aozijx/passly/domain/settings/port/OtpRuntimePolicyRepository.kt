package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.settings.model.OtpRuntimePolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Reads the current OTP policy for an action that needs one consistent snapshot. */
interface OtpRuntimePolicySnapshotReader {
    suspend fun readOtpRuntimePolicySnapshot(): OtpRuntimePolicy
}

interface OtpRuntimePolicyRepository : OtpRuntimePolicySnapshotReader {
    val otpRuntimePolicy: Flow<OtpRuntimePolicy>

    override suspend fun readOtpRuntimePolicySnapshot(): OtpRuntimePolicy = otpRuntimePolicy.first()

    suspend fun setCopyNextTotpNearExpiry(enabled: Boolean)
}
