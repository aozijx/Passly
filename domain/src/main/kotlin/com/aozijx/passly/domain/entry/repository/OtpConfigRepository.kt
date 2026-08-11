package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.otp.OtpConfig

interface OtpConfigRepository {
    suspend fun getConfig(entryId: String): OtpConfig?
}
