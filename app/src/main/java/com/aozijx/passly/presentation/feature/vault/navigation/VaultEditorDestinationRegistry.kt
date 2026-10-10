package com.aozijx.passly.presentation.feature.vault.navigation

import com.aozijx.passly.presentation.feature.shell.navigation.AppRoute
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel

internal object VaultEditorDestinationRegistry {
    private val routes = mapOf(
        VaultAddTypeUiModel.PASSWORD to AppRoute.AddPassword.route,
        VaultAddTypeUiModel.TOTP to AppRoute.AddOtp.route,
        VaultAddTypeUiModel.BANK_CARD to AppRoute.AddBankCard.route,
        VaultAddTypeUiModel.WIFI to AppRoute.AddWifi.route,
    )

    fun routeFor(type: VaultAddTypeUiModel): String? = routes[type]
}
