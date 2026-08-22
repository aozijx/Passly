package com.aozijx.passly.feature.detail.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.detail.ui.components.SensitiveFieldCard

/**
 * A purely stateless UI section for credentials (username and password).
 * Interactions are dispatched via callbacks to ensure UI purity and MVI compliance.
 */
@Composable
fun CredentialSection(
    showUsername: Boolean,
    showPassword: Boolean,
    usernameLabel: String,
    passwordLabel: String,
    revealedUsername: String?,
    revealedPassword: String?,
    editState: EntryEditState,
    onUsernameClick: () -> Unit,
    onPasswordClick: () -> Unit,
    onUsernameCopy: () -> Unit,
    onPasswordCopy: () -> Unit,
    onUsernameSave: (String) -> Unit,
    onPasswordSave: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showUsername) {
            SensitiveFieldCard(
                title = usernameLabel,
                isEditing = editState.isEditingUsername,
                editedValue = editState.editedUsername,
                revealedValue = revealedUsername,
                onEditToggle = { editState.isEditingUsername = it },
                onValueChange = { editState.editedUsername = it },
                onReveal = onUsernameClick,
                onCopy = onUsernameCopy,
                onSave = onUsernameSave
            )
        }

        if (showPassword) {
            SensitiveFieldCard(
                title = passwordLabel,
                isEditing = editState.isEditingPassword,
                editedValue = editState.editedPassword,
                revealedValue = revealedPassword,
                onEditToggle = { editState.isEditingPassword = it },
                onValueChange = { editState.editedPassword = it },
                onReveal = onPasswordClick,
                onCopy = onPasswordCopy,
                onSave = onPasswordSave
            )
        }
    }
}
