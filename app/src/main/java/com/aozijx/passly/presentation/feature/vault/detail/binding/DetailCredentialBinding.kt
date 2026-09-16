package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.asScopedSensitiveText
import com.aozijx.passly.presentation.ui.vault.detail.component.CredentialSection
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialFieldUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialFieldUiState
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialSectionEventHandler
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialSectionUiState

@Composable
internal fun DetailCredentialBinding(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val actionHandler = DetailSectionActionHandler(onAction)
    val revealedUsername = uiState.revealed(RevealedFieldKey.USERNAME)
    val revealedPassword = uiState.revealed(RevealedFieldKey.PASSWORD)

    CredentialSection(
        state = CredentialSectionUiState(
            username = CredentialFieldUiState(
                visible = entry.username.isNotBlank() || SensitiveFieldKey.PASSWORD !in uiState.sensitiveFieldKeys,
                label = stringResource(R.string.field_username),
                revealedValue = revealedUsername?.asScopedSensitiveText(),
                isEditing = uiState.fieldEdits.isEditing(RevealedFieldKey.USERNAME),
                editedValue = uiState.fieldEdits.draft(RevealedFieldKey.USERNAME),
            ),
            password = CredentialFieldUiState(
                visible = SensitiveFieldKey.PASSWORD in uiState.sensitiveFieldKeys || entry.type != EntryType.LOGIN,
                label = stringResource(R.string.password_label),
                revealedValue = revealedPassword?.asScopedSensitiveText(),
                isEditing = uiState.fieldEdits.isEditing(RevealedFieldKey.PASSWORD),
                editedValue = uiState.fieldEdits.draft(RevealedFieldKey.PASSWORD),
            ),
        ),
        eventHandler = object : CredentialSectionEventHandler {
            override fun onEditingChanged(field: CredentialFieldUiModel, editing: Boolean) {
                val key = field.revealedFieldKey
                if (editing) {
                    val initialValue = when (field) {
                        CredentialFieldUiModel.USERNAME -> revealedUsername
                            ?.let { String(it.toCharArray()) }
                            ?: entry.username
                        CredentialFieldUiModel.PASSWORD -> revealedPassword
                            ?.let { String(it.toCharArray()) }
                            .orEmpty()
                    }
                    onAction(DetailUiAction.StartFieldEdit(key, initialValue))
                } else {
                    onAction(DetailUiAction.CancelFieldEdit(key))
                }
            }

            override fun onValueChanged(field: CredentialFieldUiModel, value: String) {
                onAction(DetailUiAction.UpdateFieldDraft(field.revealedFieldKey, value))
            }

            override fun onRevealRequested(field: CredentialFieldUiModel) {
                onAction(DetailUiAction.ToggleFieldVisibility(field.revealedFieldKey))
            }

            override fun onCopyRequested(field: CredentialFieldUiModel) {
                actionHandler.copy(
                    when (field) {
                        CredentialFieldUiModel.USERNAME -> FieldKey.USERNAME
                        CredentialFieldUiModel.PASSWORD -> FieldKey.PASSWORD
                    },
                )
            }

            override fun onSaveRequested(field: CredentialFieldUiModel, value: String) {
                onAction(DetailUiAction.SaveField(field.revealedFieldKey, value))
            }
        },
    )
}

private val CredentialFieldUiModel.revealedFieldKey: String
    get() = when (this) {
        CredentialFieldUiModel.USERNAME -> RevealedFieldKey.USERNAME
        CredentialFieldUiModel.PASSWORD -> RevealedFieldKey.PASSWORD
    }
