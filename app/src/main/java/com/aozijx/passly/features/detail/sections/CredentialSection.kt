package com.aozijx.passly.features.detail.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.components.DetailItem
import com.aozijx.passly.features.detail.components.EditTextField
import com.aozijx.passly.features.detail.internal.EntryEditState

private const val TAG = "CredentialSection"

@Composable
fun CredentialSection(
    activity: FragmentActivity,
    item: VaultEntry,
    onAuthenticate: (activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    editState: EntryEditState,
    revealedUsername: String?,
    revealedPassword: String?,
    onUsernameRevealed: (String?) -> Unit,
    onPasswordRevealed: (String?) -> Unit,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    val context = LocalContext.current
    val decryptFailMsg = stringResource(R.string.vault_decrypt_failed)
    val encryptFailMsg = stringResource(R.string.vault_encrypt_failed)

    fun decryptOrToast(ciphertext: String): String? {
        return try {
            CryptoManager.decrypt(ciphertext)
        } catch (e: Exception) {
            Logcat.e(TAG, "Decrypt failed", e)
            Toast.makeText(context, decryptFailMsg, Toast.LENGTH_SHORT).show()
            null
        }
    }

    fun saveField(
        newValue: String,
        oldRevealed: String?,
        onClose: () -> Unit,
        onSuccess: (encrypted: String) -> Unit
    ) {
        if (newValue == oldRevealed) {
            onClose()
            return
        }
        try {
            val encrypted = CryptoManager.encrypt(newValue)
            onSuccess(encrypted)
            onClose()
        } catch (e: Exception) {
            Logcat.e(TAG, "Encrypt failed", e)
            Toast.makeText(context, encryptFailMsg, Toast.LENGTH_SHORT).show()
            onClose()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CredentialRow(
            label = stringResource(R.string.label_username),
            isEditing = editState.isEditingUsername,
            editedValue = editState.editedUsername,
            revealedValue = revealedUsername,
            onEditToggle = { editState.isEditingUsername = it },
            onValueChange = { editState.editedUsername = it },
            onCopy = {
                if (revealedUsername != null) {
                    ClipboardUtils.copy(context, revealedUsername)
                } else {
                    onAuthenticate(activity, "解密信息", "验证身份以复制账号") {
                        decryptOrToast(item.username)?.let { decrypted ->
                            ClipboardUtils.copy(context, decrypted)
                            onUsernameRevealed(decrypted)
                        }
                    }
                }
            },
            onSave = { newValue ->
                saveField(newValue, revealedUsername, { editState.isEditingUsername = false }) { encrypted ->
                    onEntryUpdated(item.copy(username = encrypted))
                    onUsernameRevealed(newValue)
                }
            })

        val showPassword = item.password.isNotEmpty() || item.entryType != 1
        if (showPassword) {
            CredentialRow(
                label = stringResource(R.string.label_password),
                isEditing = editState.isEditingPassword,
                editedValue = editState.editedPassword,
                revealedValue = revealedPassword,
                onEditToggle = { editState.isEditingPassword = it },
                onValueChange = { editState.editedPassword = it },
                onCopy = {
                    if (revealedPassword != null) {
                        ClipboardUtils.copy(context, revealedPassword)
                    } else {
                        onAuthenticate(activity, "解密信息", "验证身份以复制密码") {
                            decryptOrToast(item.password)?.let { decrypted ->
                                ClipboardUtils.copy(context, decrypted)
                                onPasswordRevealed(decrypted)
                            }
                        }
                    }
                },
                onSave = { newValue ->
                    saveField(newValue, revealedPassword, { editState.isEditingPassword = false }) { encrypted ->
                        onEntryUpdated(item.copy(password = encrypted))
                        onPasswordRevealed(newValue)
                    }
                })
        }

        if (revealedUsername == null || revealedPassword == null) {
            Button(
                onClick = {
                    onAuthenticate(activity, "解密信息", "验证身份以查看完整条目") {
                        if (revealedUsername == null && item.username.isNotEmpty()) {
                            decryptOrToast(item.username)?.let { onUsernameRevealed(it) }
                        }
                        if (revealedPassword == null && item.password.isNotEmpty()) {
                            decryptOrToast(item.password)?.let { onPasswordRevealed(it) }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }
    }
}

@Composable
private fun CredentialRow(
    label: String,
    isEditing: Boolean,
    editedValue: String,
    revealedValue: String?,
    onEditToggle: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    onCopy: () -> Unit,
    onSave: (String) -> Unit
) {
    if (isEditing) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EditTextField(
                value = editedValue,
                onValueChange = onValueChange,
                label = stringResource(R.string.label_edit_field, label),
                onSave = { onSave(editedValue) })
            if (revealedValue != null && editedValue != revealedValue) {
                Text(
                    stringResource(R.string.vault_edit_modified_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    } else {
        DetailItem(
            label = label,
            value = revealedValue ?: stringResource(R.string.label_hidden_mask),
            isRevealed = revealedValue != null,
            onCopy = onCopy,
            onEdit = {
                onValueChange(revealedValue ?: "")
                onEditToggle(true)
            })
    }
}
