package com.aozijx.passly.feature.vault.presentation.navigation

import com.aozijx.passly.feature.shell.presentation.navigation.AppRoute
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.domain.entry.model.EntryType

internal object VaultEditorDestinationRegistry {
    private val routes = mapOf(
        VaultAddTypeUiModel.PASSWORD to AppRoute.AddPassword.route,
        VaultAddTypeUiModel.TOTP to AppRoute.AddOtp.route,
        VaultAddTypeUiModel.BANK_CARD to AppRoute.AddBankCard.route,
        VaultAddTypeUiModel.WIFI to AppRoute.AddWifi.route,
    )

    val creatableTypes: List<VaultAddTypeUiModel> = routes.keys.toList()

    fun routeFor(type: VaultAddTypeUiModel): String? = routes[type]

    fun editRouteFor(type: EntryType, entryId: String): String? = when (type) {
        EntryType.LOGIN -> AppRoute.EditPassword.createRoute(entryId)
        EntryType.OTP -> AppRoute.EditOtp.createRoute(entryId)
        EntryType.BANK_CARD -> AppRoute.EditBankCard.createRoute(entryId)
        EntryType.WIFI -> AppRoute.EditWifi.createRoute(entryId)
        EntryType.ACCOUNT, EntryType.NOTE, EntryType.ID_CARD, EntryType.PASSPORT,
        EntryType.DRIVER_LICENSE, EntryType.SSH_KEY, EntryType.PASSKEY,
        EntryType.DATABASE_CREDENTIAL, EntryType.SERVER_CREDENTIAL, EntryType.API_KEY,
        EntryType.CRYPTO_WALLET, EntryType.SEED_PHRASE, EntryType.RECOVERY_CODE -> null
    }
}
