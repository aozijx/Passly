package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.feature.vault.detail.DetailEntryPatch
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.ui.vault.detail.component.SshKeySection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSshUiModel

@Composable
internal fun DetailSshHost(
    entry: Entry,
    uiState: DetailUiState,
    editState: EntryEditState,
    onAction: (DetailUiAction) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
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
            editState.isEditingPassword,
            editState.editedPassword,
            (hasPrivateKey && privateKey == null) || (hasPassphrase && passphrase == null),
        ),
        onFingerprintCopy = {
            handler.copy(entry.username)
            handler.record("fingerprint", ActivityType.COPY_PASSWORD)
        },
        onPassphraseCopy = {
            copySensitiveField(
                handler,
                "passphrase",
                passphrase?.let(OwnedChars::fromString),
                null,
            )
        },
        onPassphraseReveal = {
            if (passphrase != null) {
                onAction(DetailUiAction.RevealField(RevealedFieldKey.SSH_PASSPHRASE, null))
            } else {
                onAction(
                    DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SSH_PASSPHRASE),
                )
            }
        },
        onPassphraseEditStarted = {
            editState.editedPassword = passphrase.orEmpty()
            editState.isEditingPassword = true
        },
        onPassphraseChanged = { editState.editedPassword = it },
        onPassphraseSaved = {
            if (it != passphrase) {
                onAction(
                    DetailUiAction.CommitPatch(
                        DetailEntryPatch.SshPassphrase(it),
                        DetailEditCompletion.SensitiveField(RevealedFieldKey.SSH_PASSPHRASE),
                    ),
                )
            }
        },
        onPrivateKeyClick = {
            if (privateKey == null) {
                onAction(
                    DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SSH_PRIVATE_KEY),
                )
            } else {
                copySensitiveField(
                    handler,
                    "private key",
                    OwnedChars.fromString(privateKey),
                    null,
                )
            }
        },
        onRevealAll = {
            val keys = buildSet {
                if (hasPrivateKey && privateKey == null) add(RevealedFieldKey.SSH_PRIVATE_KEY)
                if (hasPassphrase && passphrase == null) add(RevealedFieldKey.SSH_PASSPHRASE)
            }
            if (keys.isNotEmpty()) onAction(DetailUiAction.RevealHighSensitivityFields(keys))
        },
    )
}
