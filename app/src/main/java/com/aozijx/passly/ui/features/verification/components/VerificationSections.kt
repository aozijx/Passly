package com.aozijx.passly.ui.features.verification.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.memory.SecureString

@Composable
internal fun PasswordUnlockSection(
    appPassword: SecureString,
    showPasswordInput: Boolean,
    authInProgress: Boolean,
    onPasswordChange: (String) -> Unit,
    onExpandInput: () -> Unit,
    onUnlockRequest: () -> Unit
) {
    AnimatedVisibility(
        visible = showPasswordInput,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        PasswordInputField(
            appPassword = appPassword,
            authInProgress = authInProgress,
            onPasswordChange = onPasswordChange,
            onUnlockRequest = onUnlockRequest
        )
    }

    if (showPasswordInput) {
        Spacer(modifier = Modifier.height(12.dp))
    }

    FilledTonalButton(
        onClick = {
            if (!showPasswordInput) onExpandInput() else onUnlockRequest()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = if (showPasswordInput) !appPassword.isEmpty && !authInProgress else !authInProgress,
        shape = RoundedCornerShape(16.dp)
    ) {
        AnimatedContent(
            targetState = showPasswordInput to authInProgress,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "button_content"
        ) { (expanded, loading) ->
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(
                            if (expanded) R.string.auth_verify_now
                            else R.string.auth_unlock_with_app_password
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordInputField(
    appPassword: SecureString,
    authInProgress: Boolean,
    onPasswordChange: (String) -> Unit,
    onUnlockRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = appPassword.toPlainString(),
        onValueChange = onPasswordChange,
        singleLine = true,
        label = { Text(stringResource(R.string.auth_app_password_label)) },
        visualTransformation = PasswordVisualTransformation(),
        trailingIcon = {
            if (authInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                IconButton(
                    onClick = onUnlockRequest,
                    enabled = !appPassword.isEmpty
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.auth_verify_now),
                        tint = if (!appPassword.isEmpty) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { if (!appPassword.isEmpty && !authInProgress) onUnlockRequest() }
        ),
        enabled = !authInProgress,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    )
}