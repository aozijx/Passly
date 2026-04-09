package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.TotpConfig
import com.aozijx.passly.domain.repository.vault.OtpRepository

/**
 * 获取 TOTP 验证码用例
 * 将 OTP 计算逻辑委托给 OtpRepository 实现依赖倒置
 */
class GetTotpCodeUseCase(private val repository: OtpRepository) {
    operator fun invoke(config: TotpConfig): String = repository.generateTotp(config)

    operator fun invoke(
        secret: String,
        digits: Int,
        period: Int,
        algorithm: String
    ): String = invoke(TotpConfig(secret = secret, digits = digits, period = period, algorithm = algorithm))
}
