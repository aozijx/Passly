package com.aozijx.passly.ui.features.settings.contract

sealed interface SettingsEffect {
    data class ShowError(val message: String) : SettingsEffect
    data object SettingsSaved : SettingsEffect
}