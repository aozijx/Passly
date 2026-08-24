package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    isEditingUsername: Boolean,
    editedUsername: String,
    isEditingPassword: Boolean,
    editedPassword: String,
    onUsernameEditToggled: (Boolean) -> Unit,
    onPasswordEditToggled: (Boolean) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
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
                isEditing = isEditingUsername,
                editedValue = editedUsername,
                revealedValue = revealedUsername,
                onEditToggle = onUsernameEditToggled,
                onValueChange = onUsernameChanged,
                onReveal = onUsernameClick,
                onCopy = onUsernameCopy,
                onSave = onUsernameSave
            )
        }

        if (showPassword) {
            SensitiveFieldCard(
                title = passwordLabel,
                isEditing = isEditingPassword,
                editedValue = editedPassword,
                revealedValue = revealedPassword,
                onEditToggle = onPasswordEditToggled,
                onValueChange = onPasswordChanged,
                onReveal = onPasswordClick,
                onCopy = onPasswordCopy,
                onSave = onPasswordSave
            )
        }
    }
}
