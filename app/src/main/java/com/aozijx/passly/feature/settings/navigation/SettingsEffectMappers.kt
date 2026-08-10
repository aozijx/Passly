package com.aozijx.passly.feature.settings.navigation

import android.content.Context
import com.aozijx.passly.R
import com.aozijx.passly.feature.settings.contract.SettingsEffect

internal fun SettingsEffect.toMessage(context: Context): String = when (this) {
    is SettingsEffect.ShowError -> message
    is SettingsEffect.SettingsSaved -> context.getString(R.string.settings_saved)
    is SettingsEffect.DatabaseCleared -> context.getString(R.string.database_cleared)
    is SettingsEffect.AppPasswordSet -> context.getString(R.string.settings_auth_password_set_success)
    is SettingsEffect.AppPasswordChanged -> context.getString(R.string.settings_auth_password_change_success)
    is SettingsEffect.AppPasswordDisabled -> context.getString(R.string.settings_auth_password_disabled)
    is SettingsEffect.AppPasswordError -> message
}