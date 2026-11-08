package com.aozijx.passly.feature.vault.presentation.list.ui.model

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel

sealed interface VaultListItemAction {
    data class Clicked(
        val item: VaultListItemUiModel,
    ) : VaultListItemAction

    data class Swiped(
        val item: VaultListItemUiModel,
        val action: SwipeActionType,
    ) : VaultListItemAction
}
