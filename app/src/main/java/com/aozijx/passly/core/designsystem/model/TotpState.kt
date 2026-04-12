package com.aozijx.passly.core.designsystem.model

/**
 * TOTP 实时显示状态（跨 feature 共享：vault 列表 + detail 详情页）
 */
data class TotpState(
    val code: String = "------",
    val progress: Float = 1f,
    val decryptedSecret: String? = null
)
