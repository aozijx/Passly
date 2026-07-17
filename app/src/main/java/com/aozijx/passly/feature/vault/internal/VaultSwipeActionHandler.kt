package com.aozijx.passly.feature.vault.internal

import com.aozijx.passly.domain.model.entry.FieldKey
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.settings.SwipeActionType

fun handleSwipeAction(
    actionType: SwipeActionType,
    item: VaultEntry,
    onAuthRequired: (onSuccess: () -> Unit) -> Unit,
    onQuickDelete: (VaultEntry) -> Unit,
    onShowDetail: (VaultEntry) -> Unit,
    onCopy: (FieldKey) -> Unit
) {
    if (actionType == SwipeActionType.DISABLED) return

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