package com.aozijx.passly.presentation.feature.vault.list.ui.model

import com.aozijx.passly.domain.settings.model.SwipeActionType

sealed interface VaultListItemAction {
    data class Clicked(val item: VaultListItemUiModel) : VaultListItemAction
    data class Swiped(
        val item: VaultListItemUiModel,
        val action: SwipeActionType,
    ) : VaultListItemAction
}
