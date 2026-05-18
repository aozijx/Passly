package com.aozijx.passly.features.settings.internal

internal object SettingsConstants {
    const val MIN_LOCK_TIMEOUT_MS = 5_000L
    const val DEFAULT_LOCK_TIMEOUT_MS = 60_000L

    const val TAB_THRESHOLD_MIN = 2
    const val TAB_THRESHOLD_MAX = 8
    const val DEFAULT_TAB_BAR_MAX_TABS = 4

    const val STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L

    val LOCK_TIMEOUT_PRESETS = listOf(
        15_000L to "15 秒",
        30_000L to "30 秒",
        60_000L to "1 分钟",
        120_000L to "2 分钟",
        300_000L to "5 分钟",
        600_000L to "10 分钟"
    )
}