package com.aozijx.passly.features.detail.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.components.DetailItem
import com.aozijx.passly.features.detail.components.InfoGroupCard
import com.aozijx.passly.features.detail.internal.EntryEditState

@Composable
fun WifiSection(
    activity: FragmentActivity,
    entry: VaultEntry,
    editState: EntryEditState,
    revealedPassword: String?,
    onPasswordRevealed: (String?) -> Unit,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onAuthenticate: (activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    val context = LocalContext.current
    val wifiSsidLabel = stringResource(R.string.wifi_ssid)
    val wifiPasswordLabel = stringResource(R.string.wifi_password)
    val wifiEncryptionLabel = stringResource(R.string.wifi_encryption)
    val wifiHiddenLabel = stringResource(R.string.wifi_hidden)
    val wifiCopiedMsg = stringResource(R.string.wifi_copied)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = wifiSsidLabel,
            value = entry.username,
            isRevealed = true,
            onCopy = { ClipboardUtils.copy(context, entry.username) },
            onEdit = {}
        )

        if (editState.isEditingPassword) {
            OutlinedTextField(
                value = editState.editedPassword,
                onValueChange = { editState.editedPassword = it },
                label = { Text(stringResource(R.string.label_edit_field, wifiPasswordLabel)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        saveEncrypted(editState.editedPassword, revealedPassword, { editState.isEditingPassword = false }) { encrypted ->
                            onEntryUpdated(entry.copy(password = encrypted))
                            onPasswordRevealed(editState.editedPassword)
                        }
                    }) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                singleLine = true
            )
        } else {
            DetailItem(
                label = wifiPasswordLabel,
                value = revealedPassword ?: stringResource(R.string.label_hidden_mask),
                isRevealed = revealedPassword != null,
                onCopy = {
                    if (revealedPassword != null) {
                        ClipboardUtils.copy(context, revealedPassword)
                        Toast.makeText(context, wifiCopiedMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        onAuthenticate(activity, "解密 WiFi 密码", "验证身份以复制密码", {
                            try {
                                val decrypted = CryptoManager.decrypt(entry.password)
                                ClipboardUtils.copy(context, decrypted)
                                Toast.makeText(context, wifiCopiedMsg, Toast.LENGTH_SHORT).show()
                                onPasswordRevealed(decrypted)
                            } catch (e: Exception) {}
                        })
                    }
                },
                onEdit = {
                    editState.editedPassword = revealedPassword ?: ""
                    editState.isEditingPassword = true
                }
            )
        }

        if (revealedPassword == null && !editState.isEditingPassword) {
            Button(
                onClick = {
                    onAuthenticate(activity, "解密 WiFi 密码", "验证身份以查看密码", {
                        try {
                            onPasswordRevealed(CryptoManager.decrypt(entry.password))
                        } catch (e: Exception) {}
                    })
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }

        InfoGroupCard(title = stringResource(R.string.wifi_settings)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(wifiEncryptionLabel, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        entry.wifiSecurityType ?: "WPA",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(wifiHiddenLabel, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = entry.wifiIsHidden,
                        onCheckedChange = null,
                        enabled = false
                    )
                }
            }
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