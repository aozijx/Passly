package com.aozijx.passly.domain.repository.otp

import com.aozijx.passly.domain.model.core.OtpConfig

interface OtpRepository {
    fun generate(config: OtpConfig): String
}
