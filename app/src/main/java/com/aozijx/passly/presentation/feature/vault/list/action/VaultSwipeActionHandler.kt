package com.aozijx.passly.presentation.feature.vault.list.action

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.settings.model.SwipeActionType

fun handleSwipeAction(
    actionType: SwipeActionType,
    item: EntryListItem,
    onDeleteAuthRequired: (onSuccess: () -> Unit) -> Unit,
    onCopyAuthRequired: (onSuccess: () -> Unit) -> Unit,
    onQuickDelete: (EntryListItem) -> Unit,
    onShowDetail: (EntryListItem) -> Unit,
    onCopy: (FieldKey) -> Unit
) {
    val copyField = when (actionType) {
        SwipeActionType.COPY_PASSWORD -> FieldKey.PASSWORD
        SwipeActionType.COPY_USERNAME -> FieldKey.USERNAME
        else -> null
    }

    val performAction = {
        when (actionType) {
            SwipeActionType.DELETE -> onQuickDelete(item)
            SwipeActionType.DETAIL -> onShowDetail(item)
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
