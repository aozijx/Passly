package com.aozijx.passly.ui.features.settings.navigation

import java.io.Serializable

/**
 * 设置模块嵌套导航路由。
 */
sealed class SettingsRoute(val route: String) : Serializable {
    data object Main : SettingsRoute("settings/main") {
        private fun readResolve(): Any = Main
    }

    data object Security : SettingsRoute("settings/security") {
        private fun readResolve(): Any = Security
    }

    data object Privacy : SettingsRoute("settings/privacy") {
        private fun readResolve(): Any = Privacy
    }

    data object Appearance : SettingsRoute("settings/appearance") {
        private fun readResolve(): Any = Appearance
    }

    data object Interface : SettingsRoute("settings/interface") {
        private fun readResolve(): Any = Interface
    }

    data object Interaction : SettingsRoute("settings/interaction") {
        private fun readResolve(): Any = Interaction
    }

    data object DataManagement : SettingsRoute("settings/data") {
        private fun readResolve(): Any = DataManagement
    }

    data object General : SettingsRoute("settings/general") {
        private fun readResolve(): Any = General
    }
}