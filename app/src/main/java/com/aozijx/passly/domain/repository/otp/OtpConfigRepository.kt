package com.aozijx.passly.domain.repository.otp

import com.aozijx.passly.domain.model.otp.OtpConfig

interface OtpConfigRepository {
    suspend fun getConfig(entryId: String): OtpConfig?
}
