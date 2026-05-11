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
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.core.VaultHistory
import com.aozijx.passly.features.detail.components.DetailItem
import com.aozijx.passly.features.detail.components.EditTextField
import com.aozijx.passly.features.detail.contract.DetailEvent
import com.aozijx.passly.features.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.features.detail.internal.EntryEditState
import com.aozijx.passly.features.detail.internal.copySensitiveField

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
    onEntryUpdated: (VaultEntry) -> Unit,
    onEvent: (DetailEvent) -> Unit
) {
    val context = LocalContext.current
    val actionHandler = DetailSectionActionHandler(
        activity = activity,
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CredentialRow(
            label = stringResource(R.string.label_username),
            isEditing = editState.isEditingUsername,
            editedValue = editState.editedUsername,
            revealedValue = revealedUsername,
            onEditToggle = { editState.isEditingUsername = it },
            onValueChange = { editState.editedUsername = it },
            onCopy = {
                copySensitiveField(
                    context = context,
                    handler = actionHandler,
                    fieldName = "username",
                    revealedValue = revealedUsername,
                    sourceValue = item.username,
                    authTitle = "解密信息",
                    authSubtitle = "验证身份以复制账号",
                    onReveal = onUsernameRevealed
                )
            },
            onSave = { newValue ->
                if (newValue != revealedUsername) {
                    onEntryUpdated(item.copy(username = newValue))
                    onUsernameRevealed(newValue)
                }
                editState.isEditingUsername = false
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
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "password",
                        revealedValue = revealedPassword,
                        sourceValue = item.password,
                        authTitle = "解密信息",
                        authSubtitle = "验证身份以复制密码",
                        onReveal = onPasswordRevealed
                    )
                },
                onSave = { newValue ->
                    if (newValue != revealedPassword) {
                        onEntryUpdated(item.copy(password = newValue))
                        onPasswordRevealed(newValue)
                    }
                    editState.isEditingPassword = false
                })
        }

        if (revealedUsername == null || revealedPassword == null) {
            Button(
                onClick = {
                    onAuthenticate(activity, "解密信息", "验证身份以查看完整条目") {
                        if (revealedUsername == null && item.username.isNotEmpty()) {
                            onUsernameRevealed(item.username)
                            onEvent(
                                DetailEvent.RecordAction(
                                    "username",
                                    VaultHistory.HistoryType.ACCESS
                                )
                            )
                        }
                        if (revealedPassword == null && item.password.isNotEmpty()) {
                            onPasswordRevealed(item.password)
                            onEvent(
                                DetailEvent.RecordAction(
                                    "password",
                                    VaultHistory.HistoryType.ACCESS
                                )
                            )
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