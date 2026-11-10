package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.model.CredentialFieldUiState
import com.aozijx.passly.feature.vault.presentation.detail.DetailField

/**
 * A purely stateless UI section for credentials (username and password).
 * Interactions are dispatched via callbacks to ensure UI purity and MVI compliance.
 */
@Composable
fun CredentialSection(
    fields: List<CredentialFieldUiState>,
    onAction: (DetailUiAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        fields.forEach { field ->
            CredentialField(field, field.field, onAction)
        }
    }
}

@Composable
private fun CredentialField(
    state: CredentialFieldUiState,
    field: DetailField,
    onAction: (DetailUiAction) -> Unit,
) {
    val revealedValue = state.revealedValue?.useChars(::String)
    SensitiveFieldCard(
        title = stringResource(state.titleRes),
        isEditing = state.isEditing,
        editedValue = state.editedValue,
        revealedValue = revealedValue,
        onEditToggle = { editing ->
            onAction(
                if (editing) {
                    DetailUiAction.StartFieldEdit(field)
                } else {
                    DetailUiAction.CancelFieldEdit(field)
                },
            )
        },
        onValueChange = { onAction(DetailUiAction.UpdateFieldDraft(field, it)) },
        onReveal = { onAction(DetailUiAction.ToggleFieldVisibility(field)) },
        onCopy = { onAction(DetailUiAction.CopyField(field)) },
        onSave = { onAction(DetailUiAction.SaveField(field, it)) },
    )
}
