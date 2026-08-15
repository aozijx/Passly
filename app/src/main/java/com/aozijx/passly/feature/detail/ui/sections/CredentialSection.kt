package com.aozijx.passly.feature.detail.ui.sections

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
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.HiddenMask
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.ui.components.DetailItem
import com.aozijx.passly.feature.detail.ui.components.EditTextField
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.detail.internal.copySensitiveField
import com.aozijx.passly.feature.detail.internal.withDetailUsername
import com.aozijx.passly.feature.detail.internal.withLoginPassword

@Composable
fun CredentialSection(
    item: Entry,
    hasPasswordField: Boolean,
    onAuthenticate: DetailAuthenticate,
    editState: EntryEditState,
    revealedUsername: String?,
    revealedPassword: String?,
    onUsernameRevealed: (String?) -> Unit,
    onPasswordRevealed: (String?) -> Unit,
    onEntryUpdated: (Entry) -> Unit,
    onEvent: (DetailIntent) -> Unit
) {
    val context = LocalContext.current
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
    )
    val hasUsername = item.username.isNotBlank()
    val hasPassword = hasPasswordField
    val showUsername = hasUsername || !hasPassword

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (showUsername) {
            CredentialRow(
                label = stringResource(R.string.field_username),
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
                        sourceValue = item.username
                    )
                },
                onSave = { newValue ->
                    if (newValue != revealedUsername) {
                        onEntryUpdated(item.withDetailUsername(newValue))
                        onUsernameRevealed(newValue)
                    }
                    editState.isEditingUsername = false
                })
        }

        val showPassword =
            hasPasswordField || item.type != EntryType.LOGIN
        if (showPassword) {
            CredentialRow(
                label = stringResource(R.string.password_label),
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
                        sourceValue = item.secret.login?.password
                    )
                },
                onSave = { newValue ->
                    val normalizedValue = newValue.trim()
                    if (normalizedValue != revealedPassword) {
                        onEntryUpdated(item.withLoginPassword(normalizedValue))
                        onPasswordRevealed(normalizedValue)
                    }
                    editState.isEditingPassword = false
                })
        }

        val hasHiddenUsername = showUsername && hasUsername && revealedUsername == null
        val hasHiddenPassword = showPassword && hasPassword && revealedPassword == null
        if (hasHiddenUsername || hasHiddenPassword) {
            Button(
                onClick = {
                    onAuthenticate.reveal {
                        if (revealedUsername == null && item.username.isNotEmpty()) {
                            onUsernameRevealed(item.username)
                            onEvent(
                                DetailIntent.RecordAction(
                                    "username",
                                    ActivityType.VIEW
                                )
                            )
                        }
                        // 密码为字段级密文：走认证后按需解密，不在 UI 层直接读取。
                        if (revealedPassword == null && hasPasswordField) {
                            onEvent(
                                DetailIntent.RevealHighSensitivityField(RevealedFieldKey.PASSWORD)
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
                label = stringResource(R.string.field_edit_action, label),
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
            value = revealedValue ?: HiddenMask.DEFAULT,
            isRevealed = revealedValue != null,
            onCopy = onCopy,
            onEdit = {
                onValueChange(revealedValue ?: "")
                onEditToggle(true)
            })
    }
}
