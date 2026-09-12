package com.aozijx.passly.presentation.feature.vault.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.aozijx.passly.presentation.ui.vault.list.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEvent
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEventHandler

internal class VaultListEventDispatcher : VaultListEventHandler {
    private var onEventCallback: (VaultListEvent) -> Unit = {}
    private var requestAuthenticationCallback: ((() -> Unit) -> Unit) = {}

    fun updateCallbacks(
        onEvent: (VaultListEvent) -> Unit,
        requestAuthentication: ((() -> Unit) -> Unit),
    ) {
        onEventCallback = onEvent
        requestAuthenticationCallback = requestAuthentication
    }

    override fun onEvent(event: VaultListEvent) = onEventCallback(event)

    override fun requestAuthentication(onSuccess: () -> Unit) =
        requestAuthenticationCallback(onSuccess)
}

@Composable
internal fun rememberVaultListEventHandler(
    onEvent: (VaultListEvent) -> Unit,
    requestAuthentication: ((() -> Unit) -> Unit),
): VaultListEventHandler {
    val dispatcher = remember { VaultListEventDispatcher() }
    SideEffect {
        dispatcher.updateCallbacks(onEvent, requestAuthentication)
    }
    return dispatcher
}

internal data class VaultListRouteCallbacks(
    val onSettingsClick: () -> Unit,
    val onAddPassword: () -> Unit,
    val onAddOtp: () -> Unit,
    val onAddBankCard: () -> Unit,
)

internal fun dispatchVaultListEvent(
    event: VaultListEvent,
    onAction: (VaultUiAction) -> Unit,
    callbacks: VaultListRouteCallbacks,
) {
    when (event) {
        VaultListEvent.SettingsClicked -> callbacks.onSettingsClick()
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
            VaultAddTypeUiModel.PASSWORD -> callbacks.onAddPassword()
            VaultAddTypeUiModel.TOTP -> callbacks.onAddOtp()
            VaultAddTypeUiModel.BANK_CARD -> callbacks.onAddBankCard()
            else -> onAction(VaultUiAction.AddTypeSelected(event.type.toFeatureModel()))
        }
        VaultListEvent.DismissAddType -> onAction(VaultUiAction.AddTypeSelected(null))
        VaultListEvent.ConfirmDelete -> onAction(VaultUiAction.ConfirmDelete)
        VaultListEvent.DismissDelete -> onAction(VaultUiAction.ItemToDeleteSelected(null))
    }
}
