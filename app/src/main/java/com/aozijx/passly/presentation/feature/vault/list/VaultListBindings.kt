package com.aozijx.passly.presentation.feature.vault.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.paging.PagingData
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel
import kotlinx.coroutines.flow.Flow

/**
 * Keeps paging inputs independent from callbacks that may change during Compose recomposition.
 */
internal class VaultListBindings(
    val entryPages: Map<VaultQuickFilterUiModel, Flow<PagingData<VaultListItemUiModel>>>,
) {
    private var onItemClick: (VaultListItemUiModel) -> Unit = {}
    private var onItemSwipe: (VaultListItemUiModel, SwipeActionUiModel) -> Unit = { _, _ -> }

    val eventHandler: VaultListItemEventHandler = object : VaultListItemEventHandler {
        override fun onClick(item: VaultListItemUiModel) = onItemClick(item)

        override fun onSwipe(item: VaultListItemUiModel, action: SwipeActionUiModel) {
            onItemSwipe(item, action)
        }
    }

    fun updateEvents(
        onItemClick: (VaultListItemUiModel) -> Unit,
        onItemSwipe: (VaultListItemUiModel, SwipeActionUiModel) -> Unit,
    ) {
        this.onItemClick = onItemClick
        this.onItemSwipe = onItemSwipe
    }
}

@Composable
internal fun rememberVaultListBindings(
    entryPages: Map<VaultQuickFilterUiModel, Flow<PagingData<VaultListItemUiModel>>>,
    onItemClick: (VaultListItemUiModel) -> Unit,
    onItemSwipe: (VaultListItemUiModel, SwipeActionUiModel) -> Unit,
): VaultListBindings {
    val bindings = remember(entryPages) { VaultListBindings(entryPages) }
    SideEffect {
        bindings.updateEvents(
            onItemClick = onItemClick,
            onItemSwipe = onItemSwipe,
        )
    }
    return bindings
}
