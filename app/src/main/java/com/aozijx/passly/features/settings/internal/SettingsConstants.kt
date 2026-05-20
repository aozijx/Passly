package com.aozijx.passly.features.settings.internal

import com.aozijx.passly.domain.config.AppDefaults

internal object SettingsConstants {
    val MIN_LOCK_TIMEOUT_MS: Long get() = AppDefaults.Security.MIN_LOCK_TIMEOUT_MS
    val DEFAULT_LOCK_TIMEOUT_MS: Long get() = AppDefaults.Security.DEFAULT_LOCK_TIMEOUT_MS
    val TAB_THRESHOLD_MIN: Int get() = AppDefaults.Vault.TAB_THRESHOLD_MIN
    val TAB_THRESHOLD_MAX: Int get() = AppDefaults.Vault.TAB_THRESHOLD_MAX
    val DEFAULT_TAB_BAR_MAX_TABS: Int get() = AppDefaults.Vault.DEFAULT_TAB_BAR_MAX_TABS
    val LOCK_TIMEOUT_PRESETS: List<Pair<Long, String>> get() = AppDefaults.Security.LOCK_TIMEOUT_PRESETS

    const val STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L
}