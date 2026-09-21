package com.aozijx.passly.presentation.feature.vault.list.action

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemUiModel

sealed interface VaultCopyRequest {
    data class Field(
        val entryId: String,
        val entryType: EntryType,
        val fieldKey: FieldKey,
    ) : VaultCopyRequest

    data class Otp(val entryId: String) : VaultCopyRequest
}

fun resolveCopyRequest(item: VaultListItemUiModel, fieldKey: FieldKey): VaultCopyRequest =
    if (fieldKey == FieldKey.PASSWORD && item.hasOtp) {
        VaultCopyRequest.Otp(item.id)
    } else {
        VaultCopyRequest.Field(
            entryId = item.id,
            entryType = EntryType.valueOf(item.entryType.name),
            fieldKey = fieldKey,
        )
    }

fun handleSwipeAction(
    actionType: SwipeActionType,
    item: VaultListItemUiModel,
    onQuickDelete: (String) -> Unit,
    onShowDetail: (String) -> Unit,
    onCopy: (FieldKey) -> Unit,
) {
    when (actionType) {
        SwipeActionType.COPY_PASSWORD -> onCopy(FieldKey.PASSWORD)
        SwipeActionType.COPY_USERNAME -> onCopy(FieldKey.USERNAME)
        SwipeActionType.DELETE -> onQuickDelete(item.id)
        SwipeActionType.DETAIL -> onShowDetail(item.id)
    }
}
