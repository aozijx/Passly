package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel

internal data class VaultNavigation(
    val onSettingsClick: () -> Unit,
    val onAddPassword: () -> Unit,
    val onAddOtp: () -> Unit,
    val onAddBankCard: () -> Unit,
    val onShowDetail: (String) -> Unit,
)

internal fun routeAddTypeSelection(
    type: VaultAddTypeUiModel,
    navigation: VaultNavigation,
    onAction: (VaultUiAction) -> Unit,
) {
    when (type) {
        VaultAddTypeUiModel.PASSWORD -> navigation.onAddPassword()
        VaultAddTypeUiModel.TOTP -> navigation.onAddOtp()
        VaultAddTypeUiModel.BANK_CARD -> navigation.onAddBankCard()
        else -> onAction(VaultUiAction.AddTypeSelected(type.toFeatureModel()))
    }
}
