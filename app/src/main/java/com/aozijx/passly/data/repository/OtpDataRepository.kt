package com.aozijx.passly.data.repository

import com.aozijx.passly.core.security.otp.TwoFAUtils
import com.aozijx.passly.domain.model.TotpConfig
import com.aozijx.passly.domain.repository.vault.OtpRepository

class OtpDataRepository : OtpRepository {
    override fun generateTotp(config: TotpConfig): String {
        val normalizedDigits = if (config.algorithm.uppercase() == "STEAM") 5 else config.digits
        return TwoFAUtils.generateTotp(
            secret = config.secret,
            digits = normalizedDigits,
            period = config.period,
            algorithm = config.algorithm
        )
    }
}
