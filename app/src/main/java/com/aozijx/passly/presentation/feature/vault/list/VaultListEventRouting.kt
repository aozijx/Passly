package com.aozijx.passly.presentation.feature.vault.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListEvent
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListEventHandler

@Composable
internal fun rememberVaultListEventHandler(
    onEvent: (VaultListEvent) -> Unit,
): VaultListEventHandler {
    val currentOnEvent = rememberUpdatedState(onEvent)
    return remember {
        object : VaultListEventHandler {
            override fun onEvent(event: VaultListEvent) = currentOnEvent.value(event)
        }
    }
}

internal data class VaultNavigation(
    val onSettingsClick: () -> Unit,
    val onAddPassword: () -> Unit,
    val onAddOtp: () -> Unit,
    val onAddBankCard: () -> Unit,
    val onShowDetail: (String) -> Unit,
)

internal fun dispatchVaultListEvent(
    event: VaultListEvent,
    onAction: (VaultUiAction) -> Unit,
    navigation: VaultNavigation,
) {
    when (event) {
        VaultListEvent.SettingsClicked -> navigation.onSettingsClick()
        is VaultListEvent.SearchQueryChanged -> onAction(
            VaultUiAction.SearchQueryChanged(event.query),
        )
        is VaultListEvent.SearchToggled -> onAction(VaultUiAction.SearchToggled(event.active))
        VaultListEvent.ClearCategory -> onAction(VaultUiAction.ClearCategory)
        VaultListEvent.ToggleTotpVisibility -> onAction(VaultUiAction.ToggleShowTotpCode)
        is VaultListEvent.CategorySelected -> onAction(
            VaultUiAction.CategorySelected(event.category),
        )
        is VaultListEvent.SortSelected -> onAction(
            VaultUiAction.SortOptionSelected(event.sort.toFeatureModel()),
        )
        is VaultListEvent.FilterToggled -> onAction(
            VaultUiAction.FilterToggled(event.filter?.toFeatureModel()),
        )
        is VaultListEvent.AddTypeSelected -> when (event.type) {
            VaultAddTypeUiModel.PASSWORD -> navigation.onAddPassword()
            VaultAddTypeUiModel.TOTP -> navigation.onAddOtp()
            VaultAddTypeUiModel.BANK_CARD -> navigation.onAddBankCard()
            else -> onAction(VaultUiAction.AddTypeSelected(event.type.toFeatureModel()))
        }
        VaultListEvent.DismissAddType -> onAction(VaultUiAction.AddTypeSelected(null))
        VaultListEvent.ConfirmDelete -> onAction(VaultUiAction.ConfirmDelete)
        VaultListEvent.DismissDelete -> onAction(VaultUiAction.ItemToDeleteSelected(null))
    }
}
