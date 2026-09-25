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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.database.reset.DatabaseResetUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatabaseResetSheet(
    state: DatabaseResetUiState,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
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
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (state.isResetComplete) {
                Text(
                    stringResource(R.string.database_reset_success),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (state.isResetting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isResetting && !state.isResetComplete,
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
                if (!state.isResetting) showConfirm = false
            },
            title = { Text(stringResource(R.string.database_reset_confirm_title)) },
            text = { Text(stringResource(R.string.database_reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    enabled = !state.isResetting,
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
                    enabled = !state.isResetting,
                    onClick = { showConfirm = false },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
