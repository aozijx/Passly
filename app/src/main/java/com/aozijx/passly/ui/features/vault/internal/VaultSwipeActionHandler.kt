package com.aozijx.passly.ui.features.vault.internal

import com.aozijx.passly.domain.model.FieldKey
import com.aozijx.passly.domain.model.SwipeActionType
import com.aozijx.passly.domain.model.VaultSummary

fun handleSwipeAction(
    actionType: SwipeActionType,
    item: VaultSummary,
    onAuthRequired: (onSuccess: () -> Unit) -> Unit,
    onQuickDelete: (VaultSummary) -> Unit,
    onShowDetail: (VaultSummary) -> Unit,
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