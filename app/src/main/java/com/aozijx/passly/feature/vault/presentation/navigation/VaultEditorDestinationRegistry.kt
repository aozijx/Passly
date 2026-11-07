package com.aozijx.passly.feature.vault.presentation.navigation

import com.aozijx.passly.app.navigation.AppRoute
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultAddTypeUiModel

internal object VaultEditorDestinationRegistry {
    private val editRouteFactories: Map<EntryType, (String) -> String> =
        mapOf(
            EntryType.LOGIN to { token -> AppRoute.EditPassword.createRoute(token) },
            EntryType.OTP to { token -> AppRoute.EditOtp.createRoute(token) },
            EntryType.BANK_CARD to { token -> AppRoute.EditBankCard.createRoute(token) },
            EntryType.WIFI to { token -> AppRoute.EditWifi.createRoute(token) },
        )

    private val routes =
        mapOf(
            VaultAddTypeUiModel.PASSWORD to AppRoute.AddPassword.route,
            VaultAddTypeUiModel.TOTP to AppRoute.AddOtp.route,
            VaultAddTypeUiModel.BANK_CARD to AppRoute.AddBankCard.route,
            VaultAddTypeUiModel.WIFI to AppRoute.AddWifi.route,
        )

    val creatableTypes: List<VaultAddTypeUiModel> = routes.keys.toList()

    fun routeFor(type: VaultAddTypeUiModel): String? = routes[type]

    fun supportsFullFormEdit(type: EntryType): Boolean = type in editRouteFactories

    fun editRouteFor(type: EntryType, seedToken: String): String? =
        editRouteFactories[type]?.invoke(seedToken)
}
