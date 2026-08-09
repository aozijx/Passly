package com.aozijx.passly.domain.entry.model

import com.aozijx.passly.core.otp.OtpError

/**
 * OTP UI 状态 —— 不包含敏感数据（secret 仅在短生命周期的生成器中存在）。
 *
 * - [code] 为 null 表示尚未生成或生成失败
 * - [progress] 为当前周期的进度（0~1），由 TotpCoordinator 在刷新时计算，
 *   Compose 层应使用 animateFloatAsState 独立实现平滑动画
 * - [error] 非 null 表示生成失败时的类型化错误
 */
data class OtpUiState(
    val code: String? = null,
    val progress: Float = 0f,
    val error: OtpError? = null
)