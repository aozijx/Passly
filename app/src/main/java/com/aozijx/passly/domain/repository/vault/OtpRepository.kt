package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.TotpConfig

/**
 * OTP 算法仓库：负责动态验证码的生成逻辑
 */
interface OtpRepository {
    /**
     * 根据配置生成当前的 TOTP 验证码
     */
    fun generateTotp(config: TotpConfig): String
}
