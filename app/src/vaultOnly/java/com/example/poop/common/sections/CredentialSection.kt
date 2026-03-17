package com.example.poop.common.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
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
import com.example.poop.MainViewModel
import com.example.poop.R
import com.example.poop.common.DetailItem
import com.example.poop.common.EditTextField
import com.example.poop.common.state.VaultEditState
import com.example.poop.data.VaultEntry
import com.example.poop.types.autofill.upgradeToSecureEntry
import com.example.poop.util.ClipboardUtils
import com.example.poop.utils.CryptoManager

@Composable
fun CredentialSection(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: MainViewModel,
    editState: VaultEditState,
    revealedUsername: String?,
    revealedPassword: String?,
    isSilentData: Boolean,
    onUsernameRevealed: (String?) -> Unit,
    onPasswordRevealed: (String?) -> Unit,
    onUpgraded: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. 账号行
        CredentialRow(
            label = stringResource(R.string.label_username),
            isEditing = editState.isEditingUsername,
            editedValue = editState.editedUsername,
            revealedValue = revealedUsername,
            onEditToggle = { editState.isEditingUsername = it },
            onValueChange = { editState.editedUsername = it },
            onCopy = {
                if (revealedUsername != null) ClipboardUtils.copy(context, revealedUsername)
                else viewModel.decryptSingle(activity, item.username) { it?.let { ClipboardUtils.copy(context, it) } }
            },
            onSave = { newValue ->
                saveEncrypted(newValue, revealedUsername, { editState.isEditingUsername = false }) { encrypted ->
                    viewModel.updateVaultEntry(item.copy(username = encrypted))
                    onUsernameRevealed(newValue)
                }
            }
        )

        // 2. 密码行 (如果是 TOTP 类型且密码为空则不显示)
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
                    if (revealedPassword != null) ClipboardUtils.copy(context, revealedPassword)
                    else viewModel.decryptSingle(activity, item.password) { it?.let { ClipboardUtils.copy(context, it) } }
                },
                onSave = { newValue ->
                    saveEncrypted(newValue, revealedPassword, { editState.isEditingPassword = false }) { encrypted ->
                        viewModel.updateVaultEntry(item.copy(password = encrypted))
                        onPasswordRevealed(newValue)
                    }
                }
            )
        }

        // 3. 验证与升级按钮
        if (revealedUsername == null || revealedPassword == null || isSilentData) {
            Button(
                onClick = {
                    val fieldsToDecrypt = listOfNotNull(item.username.takeIf { it.isNotEmpty() }, item.password.takeIf { it.isNotEmpty() })
                    viewModel.decryptMultiple(activity, fieldsToDecrypt) { results ->
                        onUsernameRevealed(results.getOrNull(0))
                        onPasswordRevealed(results.getOrNull(1))
                        if (isSilentData) {
                            upgradeToSecureEntry(item, viewModel)
                            onUpgraded()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(if (isSilentData) Icons.Default.Security else Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isSilentData) stringResource(R.string.vault_upgrade_security) 
                    else stringResource(R.string.vault_reveal_info)
                )
            }
        }
    }
}

/**
 * 封装的通用凭据行，处理显示与编辑切换
 */
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
            if (editedValue != (revealedValue ?: "")) {
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

/**
 * 辅助保存方法：处理脏检查与加密流程
 */
private fun saveEncrypted(
    newValue: String,
    oldValue: String?,
    onClose: () -> Unit,
    onSuccess: (String) -> Unit
) {
    if (newValue == oldValue) {
        onClose()
    } else {
        CryptoManager.encrypt(newValue, isSilent = true)?.let { encrypted ->
            onSuccess(encrypted)
            onClose()
        }
    }
}
