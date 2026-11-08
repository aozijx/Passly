package com.aozijx.passly.feature.vault.presentation.trash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashBottomSheetUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashEntryListUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashEntryUiModel
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashErrorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrashBottomSheet(
    state: TrashBottomSheetUiState,
    onDismiss: () -> Unit,
    onRestore: (TrashEntryUiModel) -> Unit,
    onDelete: (TrashEntryUiModel) -> Unit,
    onEmpty: () -> Unit,
    onClearError: () -> Unit,
    onRetryLoad: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<TrashEntryUiModel?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    val isBusy = state.isBusy

    ModalBottomSheet(
        onDismissRequest = {
            if (!isBusy) onDismiss()
        },
        sheetState =
            rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
        ) {
            TrashHeader(
                count = state.entryCount,
                isEmptying = state.entries == TrashEntryListUiState.Emptying,
                enabled = state.canEmpty,
                onEmpty = { confirmEmpty = true },
            )

            when (state.error) {
                TrashErrorUiState.Hidden -> {
                    Unit
                }

                is TrashErrorUiState.OperationFailed -> {
                    TrashError(
                        message = state.error.message,
                        actionLabel = stringResource(R.string.settings_trash_error_dismiss),
                        onAction = onClearError,
                    )
                }
            }

            when (val entries = state.entries) {
                TrashEntryListUiState.Loading -> {
                    TrashLoading()
                }

                TrashEntryListUiState.Emptying -> {
                    TrashLoading()
                }

                is TrashEntryListUiState.LoadFailed -> {
                    TrashError(
                        message = entries.message,
                        actionLabel = stringResource(R.string.settings_trash_retry),
                        onAction = onRetryLoad,
                    )
                }

                TrashEntryListUiState.Empty -> {
                    TrashEmpty()
                }

                is TrashEntryListUiState.Items -> {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                top = 12.dp,
                                end = 16.dp,
                                bottom = 8.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(entries.entries, key = { entry -> entry.id }) { entry ->
                            if (entries.activeEntryId == entry.id) {
                                TrashEntryOperationLoading()
                            } else {
                                TrashEntryCard(
                                    entry = entry,
                                    busy = isBusy,
                                    onRestore = { onRestore(entry) },
                                    onDelete = { pendingDelete = entry },
                                )
                            }
                        }
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
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_trash_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = { pendingDelete = null },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
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
                    pluralStringResource(
                        R.plurals.settings_trash_empty_confirm_message,
                        state.entryCount,
                        state.entryCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        confirmEmpty = false
                        onEmpty()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_trash_empty_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = { confirmEmpty = false },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrashHeader(
    count: Int,
    isEmptying: Boolean,
    enabled: Boolean,
    onEmpty: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_trash_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        if (count > 0) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = pluralStringResource(R.plurals.settings_trash_count, count, count),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        TextButton(
            enabled = enabled,
            onClick = onEmpty,
        ) {
            if (isEmptying) {
                LoadingIndicator(
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text =
                    stringResource(
                        if (isEmptying) {
                            R.string.settings_trash_emptying
                        } else {
                            R.string.settings_trash_empty_action
                        },
                    ),
                color =
                    if (enabled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrashLoading() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun TrashEmpty() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(20.dp)
                        .size(36.dp),
            )
        }
        Text(
            text = stringResource(R.string.settings_trash_empty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.settings_trash_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrashError(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
