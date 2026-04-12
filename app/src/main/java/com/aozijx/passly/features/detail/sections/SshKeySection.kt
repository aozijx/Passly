package com.aozijx.passly.features.detail.sections

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.components.DetailItem
import com.aozijx.passly.features.detail.internal.EntryEditState
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.vault.VaultViewModel

@Composable
fun SshKeySection(
    activity: FragmentActivity,
    entry: VaultEntry,
    editState: EntryEditState,
    revealedPassword: String?,
    onPasswordRevealed: (String?) -> Unit,
    vaultViewModel: VaultViewModel,
    mainViewModel: MainViewModel,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    val context = LocalContext.current
    val sshPrivateKeyLabel = stringResource(R.string.ssh_private_key)
    val passphraseLabel = stringResource(R.string.passphrase)
    val sshKeyCopiedMsg = stringResource(R.string.ssh_key_copied)

    var revealedPrivateKey by remember { mutableStateOf<String?>(null) }
    var revealedPassphrase by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = stringResource(R.string.ssh_fingerprint),
            value = entry.username.ifEmpty { stringResource(R.string.ssh_default_fingerprint) },
            isRevealed = true,
            onCopy = { ClipboardUtils.copy(context, entry.username) },
            onEdit = {}
        )

        if (editState.isEditingPassword) {
            OutlinedTextField(
                value = editState.editedPassword,
                onValueChange = { editState.editedPassword = it },
                label = { Text(stringResource(R.string.label_edit_field, passphraseLabel)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        saveEncrypted(editState.editedPassword, revealedPassphrase, { editState.isEditingPassword = false }) { encrypted ->
                            onEntryUpdated(entry.copy(password = encrypted))
                            revealedPassphrase = editState.editedPassword
                        }
                    }) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                singleLine = true
            )
        } else {
            DetailItem(
                label = passphraseLabel,
                value = revealedPassphrase ?: stringResource(R.string.label_hidden_mask),
                isRevealed = revealedPassphrase != null,
                onCopy = {
                    val passphrase = revealedPassphrase
                    if (passphrase != null) {
                        ClipboardUtils.copy(context, passphrase)
                        Toast.makeText(context, sshKeyCopiedMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        vaultViewModel.decryptSingle(activity, entry.password, mainViewModel::authenticate) { decrypted ->
                            decrypted?.let {
                                ClipboardUtils.copy(context, it)
                                Toast.makeText(context, sshKeyCopiedMsg, Toast.LENGTH_SHORT).show()
                                revealedPassphrase = it
                            }
                        }
                    }
                },
                onEdit = {
                    editState.editedPassword = revealedPassphrase ?: ""
                    editState.isEditingPassword = true
                }
            )
        }

        Surface(
            onClick = {
                val privateKey = revealedPrivateKey
                if (privateKey != null) {
                    ClipboardUtils.copy(context, privateKey)
                    Toast.makeText(context, sshKeyCopiedMsg, Toast.LENGTH_SHORT).show()
                } else {
                    val sshKey = entry.sshPrivateKey
                    if (sshKey != null) {
                        vaultViewModel.decryptSingle(activity, sshKey, mainViewModel::authenticate) { decrypted ->
                            decrypted?.let {
                                ClipboardUtils.copy(context, it)
                                Toast.makeText(context, sshKeyCopiedMsg, Toast.LENGTH_SHORT).show()
                                revealedPrivateKey = it
                            }
                        }
                    }
                }
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
                    text = if (revealedPrivateKey != null) {
                        revealedPrivateKey!!.take(60) + "..."
                    } else {
                        stringResource(R.string.label_hidden_mask)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (revealedPrivateKey != null) FontFamily.Monospace else FontFamily.Default,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }

        if (revealedPrivateKey != null) {
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
                        text = revealedPrivateKey!!,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        if (revealedPrivateKey == null && revealedPassphrase == null) {
            Button(
                onClick = {
                    val sshKey = entry.sshPrivateKey
                    if (sshKey != null) {
                        vaultViewModel.decryptSingle(activity, sshKey, mainViewModel::authenticate) { decrypted ->
                            decrypted?.let { revealedPrivateKey = it }
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
                onCopy = { ClipboardUtils.copy(context, entry.paymentPin) },
                onEdit = {}
            )
        }
    }
}

private fun saveEncrypted(
    newValue: String,
    oldValue: String?,
    onClose: () -> Unit,
    onSuccess: (String) -> Unit
) {
    if (newValue == oldValue) {
        onClose()
    } else {
        try {
            val encrypted = CryptoManager.encrypt(newValue)
            onSuccess(encrypted)
            onClose()
        } catch (e: Exception) {
            onClose()
        }
    }
}