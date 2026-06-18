package com.aozijx.passly.ui.features.settings.state

import com.aozijx.passly.domain.config.UserConfig

data class SettingsUiState(
    val security: UserConfig.Security = UserConfig.Security(),
    val display: UserConfig.Display = UserConfig.Display(),
    val vault: UserConfig.Vault = UserConfig.Vault(),
    val backup: UserConfig.Backup = UserConfig.Backup(),
    val isDarkMode: Boolean? = null,
    val isDynamicColor: Boolean = true,
)