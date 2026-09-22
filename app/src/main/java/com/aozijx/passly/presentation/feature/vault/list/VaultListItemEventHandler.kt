package com.aozijx.passly.presentation.feature.vault.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.aozijx.passly.presentation.shared.gesture.SwipeActionUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemEventHandler
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemUiModel

@Composable
internal fun rememberVaultListItemEventHandler(
    onItemClick: (VaultListItemUiModel) -> Unit,
    onItemSwipe: (VaultListItemUiModel, SwipeActionUiModel) -> Unit,
): VaultListItemEventHandler {
    val currentOnItemClick = rememberUpdatedState(onItemClick)
    val currentOnItemSwipe = rememberUpdatedState(onItemSwipe)
    return remember {
        object : VaultListItemEventHandler {
            override fun onClick(item: VaultListItemUiModel) = currentOnItemClick.value(item)

            override fun onSwipe(item: VaultListItemUiModel, action: SwipeActionUiModel) {
                currentOnItemSwipe.value(item, action)
            }
        }
    }
}
