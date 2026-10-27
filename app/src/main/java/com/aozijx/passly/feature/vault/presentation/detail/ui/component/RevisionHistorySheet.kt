package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import com.aozijx.passly.feature.vault.presentation.detail.ui.component.revision.RevisionDifferenceRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionComparisonUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionListUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionRestoreUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRevisionSheetUiModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionHistorySheet(
    model: DetailRevisionSheetUiModel,
    onAction: (DetailUiAction) -> Unit,
) {
    if (model == DetailRevisionSheetUiModel.Hidden) return
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    ModalBottomSheet(
        onDismissRequest = { onAction(DetailUiAction.DismissRevisionHistory) },
        modifier = Modifier.fillMaxHeight(0.94f),
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        when (model) {
            DetailRevisionSheetUiModel.Hidden -> Unit
            is DetailRevisionSheetUiModel.ListPage -> RevisionList(model.content, onAction)
            is DetailRevisionSheetUiModel.ComparisonPage ->
                RevisionComparison(model.content, onAction)
        }
    }
    val comparison = (model as? DetailRevisionSheetUiModel.ComparisonPage)?.content
    val restore = (comparison as? DetailRevisionComparisonUiState.Ready)?.restore
    if (restore == DetailRevisionRestoreUiState.Confirming) {
        AlertDialog(
            onDismissRequest = { onAction(DetailUiAction.CancelRevisionRestore) },
            title = { Text(stringResource(R.string.revision_restore_title)) },
            text = { Text(stringResource(R.string.revision_restore_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(DetailUiAction.ConfirmRevisionRestore) }) {
                    Text(stringResource(R.string.revision_restore_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(DetailUiAction.CancelRevisionRestore) }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun RevisionList(
    content: DetailRevisionListUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val formatter = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    Column(Modifier.fillMaxWidth().fillMaxHeight()) {
        Text(
            text = stringResource(R.string.revision_history_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        when (content) {
            DetailRevisionListUiState.Loading -> Loading()
            DetailRevisionListUiState.Failed -> Text(
                text = stringResource(R.string.revision_load_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
            is DetailRevisionListUiState.Ready -> {
                if (content.revisions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.revision_history_empty),
                        modifier = Modifier.padding(24.dp),
                    )
                } else {
                    LazyColumn {
                        items(content.revisions, key = { it.id }) { revision ->
                            ListItem(
                                supportingContent = {
                                    Text(formatter.format(Date(revision.createdAtMs)))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ListItemDefaults.colors(),
                                elevation = ListItemDefaults.elevation(),
                            ) {
                                Text(stringResource(R.string.revision_version_format, revision.version))
                            }
                            TextButton(
                                onClick = { onAction(DetailUiAction.SelectRevision(revision.id)) },
                                modifier = Modifier.padding(horizontal = 12.dp),
                            ) { Text(stringResource(R.string.revision_compare_action)) }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevisionComparison(
    content: DetailRevisionComparisonUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    Column(Modifier.fillMaxWidth().fillMaxHeight()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onAction(DetailUiAction.RevisionBack) }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            }
            Text(
                text = stringResource(R.string.revision_comparison_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
        }
        when (content) {
            DetailRevisionComparisonUiState.Loading -> Loading()
            DetailRevisionComparisonUiState.Failed -> Text(
                text = stringResource(R.string.revision_load_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
            is DetailRevisionComparisonUiState.Ready -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(content.differences, key = { it.field.toString() }) { difference ->
                        RevisionDifferenceRow(difference, onAction)
                    }
                }
                Button(
                    onClick = { onAction(DetailUiAction.RequestRevisionRestore) },
                    enabled = content.restore == DetailRevisionRestoreUiState.Idle,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                ) {
                    if (content.restore == DetailRevisionRestoreUiState.Restoring) {
                        CircularProgressIndicator()
                    } else {
                        Text(stringResource(R.string.revision_restore_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.Center,
    ) { CircularProgressIndicator() }
}
