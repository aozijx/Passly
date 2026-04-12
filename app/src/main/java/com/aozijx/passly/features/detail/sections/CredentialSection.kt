package com.aozijx.passly.features.detail.sections

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
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.components.DetailItem
import com.aozijx.passly.features.detail.components.EditTextField
import com.aozijx.passly.features.detail.internal.EntryEditState

@Composable
fun CredentialSection(
    activity: FragmentActivity,
    item: VaultEntry,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onAuthenticate: (activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    editState: EntryEditState,
    revealedUsername: String?,
    revealedPassword: String?,
    onUsernameRevealed: (String?) -> Unit,
    onPasswordRevealed: (String?) -> Unit,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    val context = LocalContext.current

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
                    onAuthenticate(activity, "解密信息", "验证身份以复制账号", {
                        // 此处解密逻辑由调用方在 View 层或注入的辅助工具处理
                        // 在当前架构下，我们将使用 CryptoManager 直接尝试（如果已通过验证）
                        try {
                            val decrypted = CryptoManager.decrypt(item.username)
                            ClipboardUtils.copy(context, decrypted)
                            onUsernameRevealed(decrypted)
                        } catch (e: Exception) {
                            // 处理解密失败
                        }
                    })
                }
            },
            onSave = { newValue ->
                saveEncrypted(newValue, revealedUsername, { editState.isEditingUsername = false }) { encrypted ->
                    onEntryUpdated(item.copy(username = encrypted))
                    onUsernameRevealed(newValue)
                }
            }
        )

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
                        onAuthenticate(activity, "解密信息", "验证身份以复制密码", {
                            try {
                                val decrypted = CryptoManager.decrypt(item.password)
                                ClipboardUtils.copy(context, decrypted)
                                onPasswordRevealed(decrypted)
                            } catch (e: Exception) {
                            }
                        })
                    }
                },
                onSave = { newValue ->
                    saveEncrypted(newValue, revealedPassword, { editState.isEditingPassword = false }) { encrypted ->
                        onEntryUpdated(item.copy(password = encrypted))
                        onPasswordRevealed(newValue)
                    }
                }
            )
        }

        if (revealedUsername == null || revealedPassword == null) {
            Button(
                onClick = {
                    onAuthenticate(activity, "解密信息", "验证身份以查看完整条目", {
                        try {
                            if (revealedUsername == null && item.username.isNotEmpty()) {
                                onUsernameRevealed(CryptoManager.decrypt(item.username))
                            }
                            if (revealedPassword == null && item.password.isNotEmpty()) {
                                onPasswordRevealed(CryptoManager.decrypt(item.password))
                            }
                        } catch (e: Exception) {
                        }
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
                onSave = { onSave(editedValue) }
            )
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
            }
        )
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