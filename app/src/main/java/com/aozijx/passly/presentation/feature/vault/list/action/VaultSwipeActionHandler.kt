package com.aozijx.passly.presentation.feature.vault.list.action

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel

fun handleSwipeAction(
    actionType: SwipeActionType,
    item: VaultListItemUiModel,
    onDeleteAuthRequired: (onSuccess: () -> Unit) -> Unit,
    onCopyAuthRequired: (onSuccess: () -> Unit) -> Unit,
    onQuickDelete: (String) -> Unit,
    onShowDetail: (String) -> Unit,
    onCopy: (FieldKey) -> Unit
) {
    val copyField = when (actionType) {
        SwipeActionType.COPY_PASSWORD -> FieldKey.PASSWORD
        SwipeActionType.COPY_USERNAME -> FieldKey.USERNAME
        else -> null
    }

    val performAction = {
        when (actionType) {
            SwipeActionType.DELETE -> onQuickDelete(item.id)
            SwipeActionType.DETAIL -> onShowDetail(item.id)
            else -> copyField?.let { onCopy(it) }
        }
    }

    when {
        actionType == SwipeActionType.DELETE ->
            onDeleteAuthRequired { performAction() }

        copyField != null ->
            onCopyAuthRequired { performAction() }

        else -> performAction()
    }
}
