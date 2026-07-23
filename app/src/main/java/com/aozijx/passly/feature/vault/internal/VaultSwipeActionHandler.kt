package com.aozijx.passly.feature.vault.internal

import com.aozijx.passly.domain.model.entry.FieldKey
import com.aozijx.passly.domain.model.lookup.EntryListItem
import com.aozijx.passly.domain.model.settings.SwipeActionType

fun handleSwipeAction(
    actionType: SwipeActionType,
    item: EntryListItem,
    onAuthRequired: (onSuccess: () -> Unit) -> Unit,
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

    if (actionType == SwipeActionType.DELETE) {
        onAuthRequired { performAction() }
    } else {
        performAction()
    }
}
