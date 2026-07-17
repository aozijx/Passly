package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.credential.twofactor.otp.OtpConfig

interface OtpRepository {
    fun generateTotp(config: OtpConfig): String
}