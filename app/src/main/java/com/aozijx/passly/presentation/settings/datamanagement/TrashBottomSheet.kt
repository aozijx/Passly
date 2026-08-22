package com.aozijx.passly.presentation.settings.datamanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.query.EntryListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrashBottomSheet(
    visible: Boolean,
    entries: List<EntryListItem>,
    isLoading: Boolean,
    activeEntryId: String?,
    isEmptying: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onRestore: (EntryListItem) -> Unit,
    onDelete: (EntryListItem) -> Unit,
    onEmpty: () -> Unit,
    onClearError: () -> Unit
) {
    if (!visible) return

    var pendingDelete by remember { mutableStateOf<EntryListItem?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    val isBusy = activeEntryId != null || isEmptying

    ModalBottomSheet(
        onDismissRequest = {
            if (!isBusy) onDismiss()
        },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            TrashHeader(
                count = entries.size,
                isEmptying = isEmptying,
                enabled = !isLoading && entries.isNotEmpty() && !isBusy,
                onEmpty = { confirmEmpty = true }
            )
            Text(
                text = stringResource(R.string.settings_trash_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            error?.let {
                TrashError(
                    message = it,
                    onDismiss = onClearError
                )
            }

            when {
                isLoading -> TrashLoading()
                entries.isEmpty() -> TrashEmpty()
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 12.dp,
                        end = 16.dp,
                        bottom = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(entries, key = { entry -> entry.id.value }) { entry ->
                        TrashEntryCard(
                            entry = entry,
                            busy = isBusy,
                            isOperating = activeEntryId == entry.id.value,
                            onRestore = { onRestore(entry) },
                            onDelete = { pendingDelete = entry }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = {
                if (!isBusy) pendingDelete = null
            },
            title = {
                Text(stringResource(R.string.settings_trash_delete_confirm_title, entry.title))
            },
            text = {
                Text(stringResource(R.string.settings_trash_delete_confirm_message))
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        pendingDelete = null
                        onDelete(entry)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.settings_trash_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = { pendingDelete = null }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = {
                if (!isBusy) confirmEmpty = false
            },
            title = { Text(stringResource(R.string.settings_trash_empty_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_trash_empty_confirm_message,
                        entries.size
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        confirmEmpty = false
                        onEmpty()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.settings_trash_empty_action),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = { confirmEmpty = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrashHeader(
    count: Int,
    isEmptying: Boolean,
    enabled: Boolean,
    onEmpty: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_trash_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (count > 0) {
                Text(
                    text = stringResource(R.string.settings_trash_count, count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(
            enabled = enabled,
            onClick = onEmpty
        ) {
            if (isEmptying) {
                LoadingIndicator(
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = stringResource(
                    if (isEmptying) {
                        R.string.settings_trash_emptying
                    } else {
                        R.string.settings_trash_empty_action
                    }
                ),
                color = if (enabled) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrashLoading() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier.padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            ContainedLoadingIndicator()
        }
    }
}

@Composable
private fun TrashEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                modifier = Modifier
                    .padding(20.dp)
                    .size(36.dp)
            )
        }
        Text(
            text = stringResource(R.string.settings_trash_empty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.settings_trash_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrashError(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_trash_error_dismiss))
            }
        }
    }
}
