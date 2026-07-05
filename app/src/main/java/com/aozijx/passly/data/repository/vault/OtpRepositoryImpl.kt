package com.aozijx.passly.data.repository.vault

import com.aozijx.passly.core.otp.TwoFAUtils
import com.aozijx.passly.domain.model.TotpConfig
import com.aozijx.passly.domain.repository.vault.OtpRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtpRepositoryImpl @Inject constructor() : OtpRepository {
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