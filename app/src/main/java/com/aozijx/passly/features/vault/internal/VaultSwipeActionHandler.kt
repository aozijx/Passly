package com.aozijx.passly.features.vault.internal

import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.domain.model.FieldKey
import com.aozijx.passly.domain.model.presentation.VaultSummary

fun handleSwipeAction(
    actionType: SwipeActionType,
    item: VaultSummary,
    onAuthRequired: (onSuccess: () -> Unit) -> Unit,
    onQuickDelete: (VaultSummary) -> Unit,
    onShowDetail: (VaultSummary) -> Unit,
    onCopy: (FieldKey) -> Unit
) {
    if (actionType == SwipeActionType.DISABLED) return

    val performAction = {
        when (actionType) {
            SwipeActionType.DELETE -> onQuickDelete(item)
            SwipeActionType.DETAIL -> onShowDetail(item)
            else -> actionType.copyField?.let { onCopy(it) }
        }
    }

    if (actionType.requiresConfirm) {
        onAuthRequired { performAction() }
    } else {
        performAction()
    }
}