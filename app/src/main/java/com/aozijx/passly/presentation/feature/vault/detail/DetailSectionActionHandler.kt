package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.FieldKey

internal class DetailSectionActionHandler(
    private val onAction: (DetailUiAction) -> Unit,
) {
    fun copy(fieldKey: FieldKey) {
        onAction(DetailUiAction.CopyField(fieldKey))
    }
}