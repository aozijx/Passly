package com.aozijx.passly.presentation.feature.vault.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.CredentialFieldUiState
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.CredentialSectionUiState
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFieldUiModel

/**
 * A purely stateless UI section for credentials (username and password).
 * Interactions are dispatched via callbacks to ensure UI purity and MVI compliance.
 */
@Composable
fun CredentialSection(
    state: CredentialSectionUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CredentialField(state.username, DetailFieldUiModel.USERNAME, onAction)
        CredentialField(state.password, DetailFieldUiModel.PASSWORD, onAction)
    }
}

@Composable
private fun CredentialField(
    state: CredentialFieldUiState,
    field: DetailFieldUiModel,
    onAction: (DetailUiAction) -> Unit,
) {
    if (!state.visible) return

    val revealedValue = state.revealedValue?.useChars(::String)
    SensitiveFieldCard(
        title = stringResource(
            when (field) {
                DetailFieldUiModel.USERNAME -> R.string.field_username
                DetailFieldUiModel.PASSWORD -> R.string.password_label
                else -> error("Unsupported credential field: $field")
            },
        ),
        isEditing = state.isEditing,
        editedValue = state.editedValue,
        revealedValue = revealedValue,
        onEditToggle = { editing ->
            onAction(
                if (editing) DetailUiAction.StartFieldEdit(field, state.valueForEditing)
                else DetailUiAction.CancelFieldEdit(field),
            )
        },
        onValueChange = { onAction(DetailUiAction.UpdateFieldDraft(field, it)) },
        onReveal = { onAction(DetailUiAction.ToggleFieldVisibility(field)) },
        onCopy = { onAction(DetailUiAction.CopyField(field)) },
        onSave = { onAction(DetailUiAction.SaveField(field, it)) },
    )
}
