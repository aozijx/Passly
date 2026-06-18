package com.aozijx.passly.ui.features.settings.state

import com.aozijx.passly.domain.config.AppDefaults
import com.aozijx.passly.domain.config.UserConfig

data class SettingsUiState(
    val security: UserConfig.Security = UserConfig.Security(),
    val display: UserConfig.Display = UserConfig.Display(),
    val vault: UserConfig.Vault = UserConfig.Vault(),
    val backup: UserConfig.Backup = UserConfig.Backup(),
) {
    companion object {
        val LOCK_TIMEOUT_PRESETS = listOf(
            AppDefaults.MIN_LOCK_TIMEOUT_MS to "${AppDefaults.MIN_LOCK_TIMEOUT_MS / 1000L} 秒",
            30_000L to "30 秒",
            60_000L to "1 分钟",
            120_000L to "2 分钟",
            300_000L to "5 分钟",
            600_000L to "10 分钟"
        )
    }
}