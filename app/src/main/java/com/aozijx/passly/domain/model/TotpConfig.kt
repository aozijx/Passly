package com.aozijx.passly.domain.model

/**
 * 纯业务的 TOTP 配置模型（不依赖 Android）。
 */
data class TotpConfig(
    val secret: String,
    val digits: Int,
    val period: Int,
    val algorithm: String,
    val issuer: String? = null,
    val label: String? = null
)
