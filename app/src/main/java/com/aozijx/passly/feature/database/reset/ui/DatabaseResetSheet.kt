package com.aozijx.passly.feature.database.reset.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.database.reset.DatabaseResetFailure
import com.aozijx.passly.feature.database.reset.DatabaseResetPhase
import com.aozijx.passly.feature.database.reset.DatabaseResetUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatabaseResetSheet(
    state: DatabaseResetUiState,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }
    val isResetting = state.phase == DatabaseResetPhase.Resetting
    val isCompleted = state.phase == DatabaseResetPhase.Completed

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.database_reset_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.database_reset_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            when (val phase = state.phase) {
                DatabaseResetPhase.Idle -> Unit
                DatabaseResetPhase.Resetting -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
                DatabaseResetPhase.Completed -> Text(
                    stringResource(R.string.database_reset_success),
                    color = MaterialTheme.colorScheme.primary,
                )
                is DatabaseResetPhase.Failed -> Text(
                    text = phase.reason.message(),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isResetting && !isCompleted,
                onClick = { showConfirm = true },
            ) {
                Text(
                    stringResource(R.string.database_reset_action),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (!isResetting) showConfirm = false
            },
            title = { Text(stringResource(R.string.database_reset_confirm_title)) },
            text = { Text(stringResource(R.string.database_reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    enabled = !isResetting,
                    onClick = {
                        showConfirm = false
                        onReset()
                    },
                ) {
                    Text(
                        stringResource(R.string.database_reset_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isResetting,
                    onClick = { showConfirm = false },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun DatabaseResetFailure.message(): String = when (this) {
    DatabaseResetFailure.SessionRestricted ->
        stringResource(R.string.database_reset_error_session_restricted)
    DatabaseResetFailure.AuthorizationDenied ->
        stringResource(R.string.database_reset_error_authorization_denied)
    is DatabaseResetFailure.Unexpected -> message
}
