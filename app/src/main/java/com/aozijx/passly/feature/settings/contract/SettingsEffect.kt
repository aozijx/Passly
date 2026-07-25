package com.aozijx.passly.feature.settings.contract

sealed interface SettingsEffect {
    data class ShowError(val message: String) : SettingsEffect
    data object SettingsSaved : SettingsEffect
    data object DatabaseCleared : SettingsEffect
}
