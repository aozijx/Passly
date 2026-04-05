package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.core.security.otp.TwoFAUtils
import com.aozijx.passly.domain.model.TotpConfig

class GetTotpCodeUseCase {
    operator fun invoke(config: TotpConfig): String {
        val normalizedDigits = if (config.algorithm.uppercase() == "STEAM") 5 else config.digits
        return TwoFAUtils.generateTotp(config.secret, normalizedDigits, config.period, config.algorithm)
    }

    operator fun invoke(
        secret: String,
        digits: Int,
        period: Int,
        algorithm: String
    ): String = invoke(TotpConfig(secret = secret, digits = digits, period = period, algorithm = algorithm))
}
