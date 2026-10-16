package com.aozijx.passly.presentation.feature.settings.main.navigation

import java.io.Serializable

/**
 * 设置模块嵌套导航路由。
 */
sealed class SettingsDestination(val route: String) : Serializable {
    companion object {
        private val destinations by lazy {
            listOf(Security, Privacy, Permissions, Appearance, Interface, Interaction, Autofill,
                DataManagement, BackupRestore, Notifications, RecoveryCode, About, Diagnostics)
                .associateBy(SettingsDestination::route)
        }

        fun fromRouteKey(routeKey: String): SettingsDestination? = destinations[routeKey]
    }
    data object Main : SettingsDestination("settings/main") {
        private fun readResolve(): Any = Main
    }

    data object Security : SettingsDestination("settings/security") {
        private fun readResolve(): Any = Security
    }

    data object Privacy : SettingsDestination("settings/privacy") {
        private fun readResolve(): Any = Privacy
    }

    data object Permissions : SettingsDestination("settings/permissions") {
        private fun readResolve(): Any = Permissions
    }

    data object Appearance : SettingsDestination("settings/appearance") {
        private fun readResolve(): Any = Appearance
    }

    data object Interface : SettingsDestination("settings/interface") {
        private fun readResolve(): Any = Interface
    }

    data object Interaction : SettingsDestination("settings/interaction") {
        private fun readResolve(): Any = Interaction
    }

    data object Autofill : SettingsDestination("settings/autofill") {
        private fun readResolve(): Any = Autofill
    }

    data object DataManagement : SettingsDestination("settings/data") {
        private fun readResolve(): Any = DataManagement
    }

    data object BackupRestore : SettingsDestination("settings/backup_restore") {
        private fun readResolve(): Any = BackupRestore
    }

    data object Notifications : SettingsDestination("settings/notifications") {
        private fun readResolve(): Any = Notifications
    }

    data object RecoveryCode : SettingsDestination("settings/recovery_code") {
        private fun readResolve(): Any = RecoveryCode
    }

    data object About : SettingsDestination("settings/about") {
        private fun readResolve(): Any = About
    }

    data object Diagnostics : SettingsDestination("settings/diagnostics") {
        private fun readResolve(): Any = Diagnostics
    }
}

internal fun settingsDetailRoutes(): List<SettingsDestination> = listOf(
    SettingsDestination.Security,
    SettingsDestination.Privacy,
    SettingsDestination.Permissions,
    SettingsDestination.Appearance,
    SettingsDestination.Interface,
    SettingsDestination.Interaction,
    SettingsDestination.Autofill,
    SettingsDestination.DataManagement,
    SettingsDestination.BackupRestore,
    SettingsDestination.Notifications,
    SettingsDestination.RecoveryCode,
    SettingsDestination.About,
    SettingsDestination.Diagnostics,
)
