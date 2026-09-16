package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.ui.vault.detail.component.SshKeySection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSshUiModel

@Composable
internal fun DetailSshBinding(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAction)
    val passphrase = uiState.revealed(RevealedFieldKey.SSH_PASSPHRASE)
        ?.let { String(it.toCharArray()) }
    val privateKey = uiState.revealed(RevealedFieldKey.SSH_PRIVATE_KEY)
        ?.let { String(it.toCharArray()) }
    val hasPassphrase = SensitiveFieldKey.SSH_PASSPHRASE in uiState.sensitiveFieldKeys
    val hasPrivateKey = SensitiveFieldKey.SSH_PRIVATE_KEY in uiState.sensitiveFieldKeys
    SshKeySection(
        model = DetailSshUiModel(
            entry.username,
            passphrase,
            passphrase != null,
            privateKey,
            privateKey != null,
            uiState.fieldEdits.isEditing(RevealedFieldKey.SSH_PASSPHRASE),
            uiState.fieldEdits.draft(RevealedFieldKey.SSH_PASSPHRASE),
            (hasPrivateKey && privateKey == null) || (hasPassphrase && passphrase == null),
        ),
        onFingerprintCopy = { handler.copy(FieldKey.USERNAME) },
        onPassphraseCopy = { handler.copy(FieldKey.SSH_PASSPHRASE) },
        onPassphraseReveal = {
            onAction(DetailUiAction.ToggleFieldVisibility(RevealedFieldKey.SSH_PASSPHRASE))
        },
        onPassphraseEditStarted = {
            onAction(DetailUiAction.StartFieldEdit(RevealedFieldKey.SSH_PASSPHRASE, passphrase.orEmpty()))
        },
        onPassphraseChanged = { onAction(DetailUiAction.UpdateFieldDraft(RevealedFieldKey.SSH_PASSPHRASE, it)) },
        onPassphraseSaved = {
            if (it != passphrase) {
                onAction(DetailUiAction.SaveField(RevealedFieldKey.SSH_PASSPHRASE, it))
            }
        },
        onPrivateKeyClick = {
            if (privateKey == null) {
                onAction(
                    DetailUiAction.ToggleFieldVisibility(RevealedFieldKey.SSH_PRIVATE_KEY),
                )
            } else {
                handler.copy(FieldKey.SSH_KEY)
            }
        },
        onRevealAll = {
            val keys = buildSet {
                if (hasPrivateKey && privateKey == null) add(RevealedFieldKey.SSH_PRIVATE_KEY)
                if (hasPassphrase && passphrase == null) add(RevealedFieldKey.SSH_PASSPHRASE)
            }
            if (keys.isNotEmpty()) onAction(DetailUiAction.RevealFields(keys))
        },
    )
}
