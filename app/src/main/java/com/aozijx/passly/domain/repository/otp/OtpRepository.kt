package com.aozijx.passly.domain.repository.otp

import com.aozijx.passly.domain.model.credential.twofactor.otp.OtpConfig

interface OtpRepository {
    fun generate(config: OtpConfig): String
}
