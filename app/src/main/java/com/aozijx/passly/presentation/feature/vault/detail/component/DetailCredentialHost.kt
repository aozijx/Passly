package com.aozijx.passly.presentation.feature.vault.detail.component

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.asScopedSensitiveText
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.ui.vault.detail.component.CredentialSection
import com.aozijx.passly.presentation.ui.vault.detail.component.TotpSection
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialFieldUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialFieldUiState
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialSectionEventHandler
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialSectionUiState
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailOtpUiModel

@Composable
internal fun DetailCredentialHost(
    entry: Entry,
    uiState: DetailUiState,
    editState: EntryEditState,
    onAction: (DetailUiAction) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
) {
    val actionHandler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
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
                when (field) {
                    CredentialFieldUiModel.USERNAME -> copySensitiveField(
                        actionHandler, "username", revealedUsername, entry.username,
                    )

                    CredentialFieldUiModel.PASSWORD -> copySensitiveField(
                        actionHandler, "password", revealedPassword, entry.secret.login?.password,
                    )
                }
            }

            override fun onSaveRequested(field: CredentialFieldUiModel, value: String) {
                onAction(DetailUiAction.SaveField(field.revealedFieldKey, value))
            }
        },
    )
}

@Composable
internal fun DetailOtpHost(
    otp: DetailOtpUiModel?,
    otpQrUri: String?,
    onAction: (DetailUiAction) -> Unit,
    onCopySensitive: (String) -> Unit,
    onOtpQrDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val copySuccessMessage = stringResource(
        R.string.field_copy_success_message,
        stringResource(R.string.vault_detail_totp_label),
    )
    TotpSection(
        currentState = otp,
        totpUri = otpQrUri,
        onQrClick = { onAction(DetailUiAction.ExportOtpQr) },
        onQrDismiss = onOtpQrDismiss,
        onCodeClick = {
            otp?.code?.takeIf { it.isNotEmpty() && !it.contains("-") }?.let { code ->
                onCopySensitive(code)
                Toast.makeText(context, copySuccessMessage, Toast.LENGTH_SHORT).show()
                onAction(DetailUiAction.RecordAction("totp", ActivityType.COPY_PASSWORD))
            }
        },
    )
}

private val CredentialFieldUiModel.revealedFieldKey: String
    get() = when (this) {
        CredentialFieldUiModel.USERNAME -> RevealedFieldKey.USERNAME
        CredentialFieldUiModel.PASSWORD -> RevealedFieldKey.PASSWORD
    }
