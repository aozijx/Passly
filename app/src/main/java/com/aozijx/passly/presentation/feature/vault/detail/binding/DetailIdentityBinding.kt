package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.ui.vault.detail.component.IdCardSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIdentityUiModel

@Composable
internal fun DetailIdentityBinding(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val idNumber = uiState.revealed(RevealedFieldKey.ID_NUMBER)?.let { String(it.toCharArray()) }
    IdCardSection(
        model = DetailIdentityUiModel(
            SensitiveFieldKey.IDENTITY_NUMBER in uiState.sensitiveFieldKeys,
            idNumber,
            idNumber != null,
            entry.username,
        ),
        onIdNumberCopy = { onAction(DetailUiAction.CopyField(FieldKey.ID_NUMBER)) },
        onIdNumberReveal = {
            onAction(DetailUiAction.ToggleFieldVisibility(RevealedFieldKey.ID_NUMBER))
        },
        onUsernameCopy = { onAction(DetailUiAction.CopyField(FieldKey.USERNAME)) },
    )
}
