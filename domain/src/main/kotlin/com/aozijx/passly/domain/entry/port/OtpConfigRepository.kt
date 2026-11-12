package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationMaterial
import com.aozijx.passly.domain.entry.otp.generation.OtpResult

interface OtpConfigRepository {
    /** Returned seed material is caller-owned and must be closed after generation. */
    suspend fun getGenerationMaterial(entryId: String): OtpGenerationMaterial?

    /** Generates and consumes the displayed HOTP counter atomically if it is still current. */
    suspend fun consumeHotpCode(
        entryId: String,
        expectedCounter: Long,
    ): OtpResult

    /** Rewinds one recently consumed HOTP value only if the persisted counter still matches. */
    suspend fun rollbackHotpCounter(
        entryId: String,
        expectedCurrentCounter: Long,
    ): Boolean
}
