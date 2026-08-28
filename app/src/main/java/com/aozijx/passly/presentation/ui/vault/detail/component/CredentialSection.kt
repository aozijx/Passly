package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialFieldUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialFieldUiState
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialSectionEventHandler
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialSectionUiState

/**
 * A purely stateless UI section for credentials (username and password).
 * Interactions are dispatched via callbacks to ensure UI purity and MVI compliance.
 */
@Composable
fun CredentialSection(
    state: CredentialSectionUiState,
    eventHandler: CredentialSectionEventHandler,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CredentialField(state.username, CredentialFieldUiModel.USERNAME, eventHandler)
        CredentialField(state.password, CredentialFieldUiModel.PASSWORD, eventHandler)
    }
}

@Composable
private fun CredentialField(
    state: CredentialFieldUiState,
    field: CredentialFieldUiModel,
    eventHandler: CredentialSectionEventHandler,
) {
    if (!state.visible) return

    val revealedValue = state.revealedValue?.useChars(::String)
    SensitiveFieldCard(
        title = state.label,
        isEditing = state.isEditing,
        editedValue = state.editedValue,
        revealedValue = revealedValue,
        onEditToggle = { eventHandler.onEditingChanged(field, it) },
        onValueChange = { eventHandler.onValueChanged(field, it) },
        onReveal = { eventHandler.onRevealRequested(field) },
        onCopy = { eventHandler.onCopyRequested(field) },
        onSave = { eventHandler.onSaveRequested(field, it) },
    )
}
