package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.otp.OtpResult

interface OtpConfigRepository {
    suspend fun getConfig(entryId: String): OtpConfig?

    /** Generates and consumes one HOTP counter atomically with its persisted next counter. */
    suspend fun consumeHotpCode(entryId: String): OtpResult
}
