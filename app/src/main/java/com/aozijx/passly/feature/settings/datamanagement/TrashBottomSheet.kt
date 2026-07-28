package com.aozijx.passly.feature.settings.datamanagement

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.VaultItemIcon
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem

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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                        .fillMaxWidth()
                        .heightIn(max = 560.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = 8.dp
                    )
                ) {
                    items(entries, key = EntryListItem::id) { entry ->
                        TrashEntryRow(
                            entry = entry,
                            busy = isBusy,
                            isOperating = activeEntryId == entry.id,
                            onRestore = { onRestore(entry) },
                            onDelete = { pendingDelete = entry }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
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
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
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

@Composable
private fun TrashEntryRow(
    entry: EntryListItem,
    busy: Boolean,
    isOperating: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val deletedLabel = entry.deletedAt?.let { deletedAt ->
        DateUtils.getRelativeTimeSpanString(
            deletedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }.orEmpty()

    ListItem(
        headlineContent = {
            Text(
                text = entry.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text(entry.entryType.displayName)
                if (deletedLabel.isNotEmpty()) {
                    Text(stringResource(R.string.settings_trash_deleted_at, deletedLabel))
                }
            }
        },
        leadingContent = {
            VaultItemIcon(iconable = entry)
        },
        trailingContent = {
            if (isOperating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Row {
                    IconButton(
                        enabled = !busy,
                        onClick = onRestore
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestoreFromTrash,
                            contentDescription = stringResource(R.string.settings_trash_restore)
                        )
                    }
                    IconButton(
                        enabled = !busy,
                        onClick = onDelete
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = stringResource(R.string.settings_trash_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun TrashLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun TrashEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.DeleteSweep,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.settings_trash_empty),
            style = MaterialTheme.typography.bodyLarge,
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
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
