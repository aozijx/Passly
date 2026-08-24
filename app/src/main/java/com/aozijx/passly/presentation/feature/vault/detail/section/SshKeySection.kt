package com.aozijx.passly.presentation.feature.vault.detail.section

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.ui.components.MaskedText
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.component.DetailItem
import com.aozijx.passly.presentation.feature.vault.detail.component.EditTextField
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.withSshPassphrase
import com.aozijx.passly.domain.sensitive.OwnedChars

@Composable
fun SshKeySection(
    entry: Entry,
    editState: EntryEditState,
    hasPassphrase: Boolean,
    hasPrivateKey: Boolean,
    revealedPassword: String?,
    revealedSshPrivateKey: String?,
    onPasswordRevealed: (String?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onEntryUpdated: (Entry) -> Unit,
    onAction: (DetailUiAction) -> Unit
) {
    val context = LocalContext.current
    val msgCopySuccess = stringResource(R.string.field_copy_success_message)
    val sshPrivateKeyLabel = stringResource(R.string.ssh_private_key)
    val passphraseLabel = stringResource(R.string.passphrase)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onAction = onAction
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = stringResource(R.string.ssh_fingerprint),
            value = entry.username.ifEmpty { stringResource(R.string.not_set) },
            isRevealed = true,
            onCopy = {
                ClipboardUtils.copy(context, entry.username)
                actionHandler.record("fingerprint", ActivityType.COPY_PASSWORD)
            },
            onEdit = {}
        )

        if (editState.isEditingPassword) {
            EditTextField(
                value = editState.editedPassword,
                onValueChange = { editState.editedPassword = it },
                label = stringResource(R.string.field_edit_action, passphraseLabel),
                onSave = {
                    if (editState.editedPassword != revealedPassword) {
                        onEntryUpdated(entry.withSshPassphrase(editState.editedPassword))
                        onPasswordRevealed(editState.editedPassword)
                    }
                    editState.isEditingPassword = false
                }
            )
        } else {
            DetailItem(
                label = passphraseLabel,
                value = revealedPassword,
                isRevealed = revealedPassword != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "passphrase",
                        revealedValue = revealedPassword?.let { OwnedChars.fromString(it) },
                        sourceValue = null,
                        afterCopy = {
                            Toast.makeText(
                                context,
                                msgCopySuccess.format(sshPrivateKeyLabel),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                },
                onEdit = {
                    editState.editedPassword = revealedPassword ?: ""
                    editState.isEditingPassword = true
                },
                onReveal = {
                    if (revealedPassword != null) onPasswordRevealed(null)
                    else onAction(
                        DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SSH_PASSPHRASE)
                    )
                }
            )
        }

        Surface(
            onClick = {
                if (revealedSshPrivateKey == null) {
                    onAction(
                        DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SSH_PRIVATE_KEY)
                    )
                } else {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "private key",
                        revealedValue = revealedSshPrivateKey.let { OwnedChars.fromString(it) },
                        sourceValue = null,
                        afterCopy = {
                            Toast.makeText(
                                context,
                                msgCopySuccess.format(sshPrivateKeyLabel),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = sshPrivateKeyLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                MaskedText(
                    text = revealedSshPrivateKey?.take(60)?.plus("..."),
                    isRevealed = revealedSshPrivateKey != null,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }

        if (revealedSshPrivateKey != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.3f
                    )
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.ssh_private_key_full),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = revealedSshPrivateKey,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        if ((hasPrivateKey && revealedSshPrivateKey == null) ||
            (hasPassphrase && revealedPassword == null)
        ) {
            Button(
                onClick = {
                    val keys = buildSet {
                        if (hasPrivateKey && revealedSshPrivateKey == null) {
                            add(RevealedFieldKey.SSH_PRIVATE_KEY)
                        }
                        if (hasPassphrase && revealedPassword == null) {
                            add(RevealedFieldKey.SSH_PASSPHRASE)
                        }
                    }
                    if (keys.isNotEmpty()) {
                        onAction(DetailUiAction.RevealHighSensitivityFields(keys))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }

        // paymentPin is not present in SshCredential; skipped for SSH key type.
    }
}
