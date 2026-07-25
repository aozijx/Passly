package com.aozijx.passly.feature.detail.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.HiddenMask
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.components.DetailItem
import com.aozijx.passly.feature.detail.components.EditTextField
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.detail.internal.copySensitiveField

@Composable
fun SshKeySection(
    entry: VaultEntry,
    editState: EntryEditState,
    revealedPassword: String?,
    revealedSshPrivateKey: String?,
    onPasswordRevealed: (String?) -> Unit,
    onSshPrivateKeyRevealed: (String?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onEntryUpdated: (VaultEntry) -> Unit,
    onEvent: (DetailIntent) -> Unit
) {
    val context = LocalContext.current
    val sshPrivateKeyLabel = stringResource(R.string.ssh_private_key)
    val passphraseLabel = stringResource(R.string.passphrase)
    val sshKeyCopiedMsg = stringResource(R.string.ssh_key_copied)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
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
                label = stringResource(R.string.edit_field, passphraseLabel),
                onSave = {
                    if (editState.editedPassword != revealedPassword) {
                        onEntryUpdated(
                            entry.copy(
                                secret = entry.secret.copy(
                                    ssh = entry.secret.ssh?.copy(
                                        passphrase = editState.editedPassword
                                    )
                                )
                            )
                        )
                        onPasswordRevealed(editState.editedPassword)
                    }
                    editState.isEditingPassword = false
                }
            )
        } else {
            DetailItem(
                label = passphraseLabel,
                value = revealedPassword ?: HiddenMask.DEFAULT,
                isRevealed = revealedPassword != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "passphrase",
                        revealedValue = revealedPassword,
                        sourceValue = entry.secret.ssh?.passphrase,
                        authTitle = "解密 SSH 密码",
                        authSubtitle = "验证身份以复制信息",
                        onReveal = onPasswordRevealed,
                        afterCopy = {
                            Toast.makeText(context, sshKeyCopiedMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onEdit = {
                    editState.editedPassword = revealedPassword ?: ""
                    editState.isEditingPassword = true
                }
            )
        }

        Surface(
            onClick = {
                copySensitiveField(
                    context = context,
                    handler = actionHandler,
                    fieldName = "private key",
                    revealedValue = revealedSshPrivateKey,
                    sourceValue = entry.secret.ssh?.privateKey,
                    authTitle = "解密 SSH 私钥",
                    authSubtitle = "验证身份以复制信息",
                    onReveal = onSshPrivateKeyRevealed,
                    afterCopy = {
                        Toast.makeText(context, sshKeyCopiedMsg, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = sshPrivateKeyLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (revealedSshPrivateKey != null) {
                        revealedSshPrivateKey.take(60) + "..."
                    } else {
                        HiddenMask.DEFAULT
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (revealedSshPrivateKey != null) FontFamily.Monospace else FontFamily.Default,
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
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

        if (revealedSshPrivateKey == null && revealedPassword == null) {
            Button(
                onClick = {
                    val sshKey = entry.secret.ssh?.privateKey
                    if (!sshKey.isNullOrBlank()) {
                        onAuthenticate {
                            onSshPrivateKeyRevealed(sshKey)
                            onEvent(
                                DetailIntent.RecordAction(
                                    "private key",
                                    ActivityType.VIEW
                                )
                            )
                            val passphrase = entry.secret.ssh.passphrase
                            if (!passphrase.isNullOrEmpty()) {
                                onPasswordRevealed(passphrase)
                                actionHandler.record("passphrase", ActivityType.VIEW)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }

        // paymentPin is not present in SshSecret; skipped for SSH key type.
    }
}
