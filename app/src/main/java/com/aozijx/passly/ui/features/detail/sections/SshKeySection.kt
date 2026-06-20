package com.aozijx.passly.ui.features.detail.sections

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
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.ui.features.detail.components.DetailItem
import com.aozijx.passly.ui.features.detail.components.EditTextField
import com.aozijx.passly.ui.features.detail.contract.DetailEvent
import com.aozijx.passly.ui.features.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.ui.features.detail.internal.EntryEditState
import com.aozijx.passly.ui.features.detail.internal.copySensitiveField

@Composable
fun SshKeySection(
    activity: FragmentActivity,
    entry: VaultEntry,
    editState: EntryEditState,
    revealedPassword: String?,
    revealedSshPrivateKey: String?,
    onPasswordRevealed: (String?) -> Unit,
    onSshPrivateKeyRevealed: (String?) -> Unit,
    onAuthenticate: (activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    onEntryUpdated: (VaultEntry) -> Unit,
    onEvent: (DetailEvent) -> Unit
) {
    val context = LocalContext.current
    val sshPrivateKeyLabel = stringResource(R.string.ssh_private_key)
    val passphraseLabel = stringResource(R.string.passphrase)
    val sshKeyCopiedMsg = stringResource(R.string.ssh_key_copied)
    val actionHandler = DetailSectionActionHandler(
        activity = activity,
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = stringResource(R.string.ssh_fingerprint),
            value = entry.username.ifEmpty { stringResource(R.string.ssh_default_fingerprint) },
            isRevealed = true,
            onCopy = {
                ClipboardUtils.copy(context, entry.username)
                actionHandler.record("fingerprint", VaultHistory.HistoryType.COPY)
            },
            onEdit = {}
        )

        if (editState.isEditingPassword) {
            EditTextField(
                value = editState.editedPassword,
                onValueChange = { editState.editedPassword = it },
                label = stringResource(R.string.label_edit_field, passphraseLabel),
                onSave = {
                    if (editState.editedPassword != revealedPassword) {
                        onEntryUpdated(entry.copy(password = editState.editedPassword))
                        onPasswordRevealed(editState.editedPassword)
                    }
                    editState.isEditingPassword = false
                }
            )
        } else {
            DetailItem(
                label = passphraseLabel,
                value = revealedPassword ?: stringResource(R.string.label_hidden_mask),
                isRevealed = revealedPassword != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "passphrase",
                        revealedValue = revealedPassword,
                        sourceValue = entry.password,
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
                    sourceValue = entry.sshPrivateKey,
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
                        stringResource(R.string.label_hidden_mask)
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
                    val sshKey = entry.sshPrivateKey
                    if (!sshKey.isNullOrBlank()) {
                        onAuthenticate(activity, "解密 SSH 私钥", "验证身份以查看完整条目") {
                            onSshPrivateKeyRevealed(sshKey)
                            onEvent(
                                DetailEvent.RecordAction(
                                    "private key",
                                    VaultHistory.HistoryType.ACCESS
                                )
                            )
                            if (entry.password.isNotEmpty()) {
                                onPasswordRevealed(entry.password)
                                actionHandler.record("passphrase", VaultHistory.HistoryType.ACCESS)
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

        if (entry.paymentPin != null) {
            DetailItem(
                label = stringResource(R.string.ssh_key_pin),
                value = entry.paymentPin,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(context, entry.paymentPin)
                    actionHandler.record("SSH key PIN", VaultHistory.HistoryType.COPY)
                },
                onEdit = {}
            )
        }
    }
}