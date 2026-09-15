package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.ui.vault.detail.component.IdCardSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIdentityUiModel

@Composable
internal fun DetailIdentityHost(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAction)
    val idNumber = uiState.revealed(RevealedFieldKey.ID_NUMBER)?.let { String(it.toCharArray()) }
    IdCardSection(
        model = DetailIdentityUiModel(
            SensitiveFieldKey.IDENTITY_NUMBER in uiState.sensitiveFieldKeys,
            idNumber,
            idNumber != null,
            entry.username,
        ),
        onIdNumberCopy = { handler.copy(FieldKey.ID_NUMBER) },
        onIdNumberReveal = {
            if (idNumber != null) {
                onAction(DetailUiAction.RevealField(RevealedFieldKey.ID_NUMBER, null))
            } else {
                onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.ID_NUMBER))
            }
        },
        onUsernameCopy = { handler.copy(FieldKey.USERNAME) },
    )
}
