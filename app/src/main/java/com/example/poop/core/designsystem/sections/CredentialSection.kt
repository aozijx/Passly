package com.example.poop.core.designsystem.sections

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
import com.example.poop.MainViewModel
import com.example.poop.R
import com.example.poop.core.designsystem.detail.DetailItem
import com.example.poop.core.designsystem.detail.EditTextField
import com.example.poop.core.designsystem.state.VaultEditState
import com.example.poop.core.crypto.CryptoManager
import com.example.poop.data.model.VaultEntry
import com.example.poop.features.vault.VaultViewModel
import com.example.poop.util.ClipboardUtils

@Composable
fun CredentialSection(
    activity: FragmentActivity,
    item: VaultEntry,
    vaultViewModel: VaultViewModel,
    mainViewModel: MainViewModel,
    editState: VaultEditState,
    revealedUsername: String?,
    revealedPassword: String?,
    onUsernameRevealed: (String?) -> Unit,
    onPasswordRevealed: (String?) -> Unit
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
                if (revealedUsername != null) ClipboardUtils.copy(context, revealedUsername)
                else vaultViewModel.decryptSingle(activity, item.username, mainViewModel::authenticate) { 
                    it?.let { ClipboardUtils.copy(context, it) } 
                }
            },
            onSave = { newValue ->
                saveEncrypted(newValue, revealedUsername, { editState.isEditingUsername = false }) { encrypted ->
                    vaultViewModel.updateVaultEntry(item.copy(username = encrypted))
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
                    if (revealedPassword != null) ClipboardUtils.copy(context, revealedPassword)
                    else vaultViewModel.decryptSingle(activity, item.password, mainViewModel::authenticate) { 
                        it?.let { ClipboardUtils.copy(context, it) } 
                    }
                },
                onSave = { newValue ->
                    saveEncrypted(newValue, revealedPassword, { editState.isEditingPassword = false }) { encrypted ->
                        vaultViewModel.updateVaultEntry(item.copy(password = encrypted))
                        onPasswordRevealed(newValue)
                    }
                }
            )
        }

        if (revealedUsername == null || revealedPassword == null) {
            Button(
                onClick = {
                    val fieldsToDecrypt = mutableListOf<String>()
                    if (revealedUsername == null && item.username.isNotEmpty()) {
                        fieldsToDecrypt.add(item.username)
                    }
                    if (revealedPassword == null && item.password.isNotEmpty()) {
                        fieldsToDecrypt.add(item.password)
                    }
                    vaultViewModel.decryptMultiple(activity, fieldsToDecrypt, mainViewModel::authenticate) { results ->
                        var resultIndex = 0
                        if (revealedUsername == null && item.username.isNotEmpty()) {
                            onUsernameRevealed(results.getOrNull(resultIndex++))
                        }
                        if (revealedPassword == null && item.password.isNotEmpty()) {
                            onPasswordRevealed(results.getOrNull(resultIndex))
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
        }
    }
}
