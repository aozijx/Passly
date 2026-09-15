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
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
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
    editState: EntryEditState,
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
                isEditing = editState.isEditingUsername,
                editedValue = editState.editedUsername,
            ),
            password = CredentialFieldUiState(
                visible = SensitiveFieldKey.PASSWORD in uiState.sensitiveFieldKeys || entry.type != EntryType.LOGIN,
                label = stringResource(R.string.password_label),
                revealedValue = revealedPassword?.asScopedSensitiveText(),
                isEditing = editState.isEditingPassword,
                editedValue = editState.editedPassword,
            ),
        ),
        eventHandler = object : CredentialSectionEventHandler {
            override fun onEditingChanged(field: CredentialFieldUiModel, editing: Boolean) {
                when (field) {
                    CredentialFieldUiModel.USERNAME -> editState.isEditingUsername = editing
                    CredentialFieldUiModel.PASSWORD -> editState.isEditingPassword = editing
                }
            }

            override fun onValueChanged(field: CredentialFieldUiModel, value: String) {
                when (field) {
                    CredentialFieldUiModel.USERNAME -> editState.editedUsername = value
                    CredentialFieldUiModel.PASSWORD -> editState.editedPassword = value
                }
            }

            override fun onRevealRequested(field: CredentialFieldUiModel) {
                onAction(DetailUiAction.ToggleVisibility(field.revealedFieldKey))
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
