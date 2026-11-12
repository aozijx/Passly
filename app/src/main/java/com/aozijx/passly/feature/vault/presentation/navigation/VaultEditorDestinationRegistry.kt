package com.aozijx.passly.feature.vault.presentation.navigation

import com.aozijx.passly.app.navigation.AppRoute
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.model.AddType

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
            AddType.PASSWORD to AppRoute.AddPassword.route,
            AddType.TOTP to AppRoute.AddOtp.route,
            AddType.BANK_CARD to AppRoute.AddBankCard.route,
            AddType.WIFI to AppRoute.AddWifi.route,
        )

    fun routeFor(type: AddType): String? = routes[type]

    fun editRouteFor(
        type: EntryType,
        seedToken: String,
    ): String? = editRouteFactories[type]?.invoke(seedToken)
}
