package com.aozijx.passly.domain.repository.otp

import com.aozijx.passly.core.otp.OtpResult
import com.aozijx.passly.domain.model.core.OtpConfig

interface OtpRepository {
    /**
     * 直接从加密 Credential 存储读取并解密完整 OTP 配置。
     * 调用方只传 entryId，不从列表摘要重建 type/secret/encoding。
     */
    suspend fun getConfig(entryId: String): OtpConfig?

    /**
     * 生成 OTP 验证码。
     *
     * @param config OTP 配置
     * @param overrideCounter 可选覆盖 counter（用于 HOTP 手动指定）
     * @param timestamp 可选 Unix 时间戳（秒），默认当前系统时间
     * @return [OtpResult.Success] 包含验证码，或 [OtpResult.Failure] 包含类型化错误
     */
    fun generate(
        config: OtpConfig,
        overrideCounter: Long? = null,
        timestamp: Long = System.currentTimeMillis() / 1000
    ): OtpResult
}
