package com.aozijx.passly.data.repository.otp

import com.aozijx.passly.core.util.TwoFAUtils
import com.aozijx.passly.domain.model.core.OtpConfig
import com.aozijx.passly.domain.repository.otp.OtpRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtpRepositoryImpl @Inject constructor() : OtpRepository {
    override fun generate(config: OtpConfig): String {
        val normalizedDigits = if (config.algorithm.uppercase() == "STEAM") 5 else config.digits
        return TwoFAUtils.generateTotp(
            secret = config.secret,
            digits = normalizedDigits,
            period = config.period,
            algorithm = config.algorithm
        )
    }
}
