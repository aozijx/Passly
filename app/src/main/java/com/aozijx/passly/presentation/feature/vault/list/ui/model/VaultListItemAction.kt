package com.aozijx.passly.presentation.feature.vault.list.ui.model

import com.aozijx.passly.presentation.shared.gesture.SwipeActionUiModel

sealed interface VaultListItemAction {
    data class Clicked(val item: VaultListItemUiModel) : VaultListItemAction
    data class Swiped(
        val item: VaultListItemUiModel,
        val action: SwipeActionUiModel,
    ) : VaultListItemAction
}
