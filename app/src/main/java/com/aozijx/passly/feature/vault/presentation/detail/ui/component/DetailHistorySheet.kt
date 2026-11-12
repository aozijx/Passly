package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.passlySheetTitleStyle
import com.aozijx.passly.feature.vault.presentation.detail.DetailHistoryFilter
import com.aozijx.passly.feature.vault.presentation.detail.DetailRevisionRestorePhase
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryAffectedFieldsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryDescriptionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryIcon
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryListUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryLoadState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistoryRevisionReference
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHistorySheetUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionComparisonContentUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRevisionComparisonUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.blocksDismissal
import com.aozijx.passly.feature.vault.presentation.detail.model.restoreConfirmationVisible
import com.aozijx.passly.feature.vault.presentation.detail.model.restoreEnabled
import com.aozijx.passly.feature.vault.presentation.detail.model.restoreInProgress
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.revision.RevisionDifferenceRow
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailHistorySheet(
    model: DetailHistorySheetUiModel,
    onAction: (DetailUiAction) -> Unit,
) {
    if (model == DetailHistorySheetUiModel.Hidden) return
    val dismissAllowed = rememberUpdatedState(!model.blocksDismissal)
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            confirmValueChange = { target -> target != SheetValue.Hidden || dismissAllowed.value },
        )
    ModalBottomSheet(
        onDismissRequest = {
            if (dismissAllowed.value) onAction(DetailUiAction.DismissRevisionHistory)
        },
        modifier = Modifier.testTag("detail-history-sheet"),
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec())
                    .fillMaxWidth(),
        ) {
            when (model) {
                DetailHistorySheetUiModel.Hidden -> {
                    Unit
                }

                is DetailHistorySheetUiModel.ListPage -> {
                    RevisionList(model.content, onAction)
                }

                is DetailHistorySheetUiModel.ComparisonPage -> {
                    RevisionComparison(model.content, onAction)
                }
            }
        }
    }
    if (model.restoreConfirmationVisible) {
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
private fun ColumnScope.RevisionList(
    content: DetailHistoryListUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale) }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).testTag("detail-history-list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("title") {
            Text(
                text = stringResource(R.string.revision_history_title),
                style = passlySheetTitleStyle(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
        item("filters") {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryFilterChip(content.filter, DetailHistoryFilter.ALL, R.string.vault_detail_history_filter_all, onAction)
                HistoryFilterChip(
                    content.filter,
                    DetailHistoryFilter.CONTENT_CHANGES,
                    R.string.vault_detail_history_filter_content,
                    onAction,
                )
                HistoryFilterChip(content.filter, DetailHistoryFilter.LIFECYCLE, R.string.vault_detail_history_filter_lifecycle, onAction)
                HistoryFilterChip(content.filter, DetailHistoryFilter.USAGE, R.string.vault_detail_history_filter_usage, onAction)
            }
        }
        if (content.isLoading) item("history-loading") { Loading() }
        if (content.activitySource == DetailHistoryLoadState.FAILED) {
            item("activity-failed") {
                HistorySourceFailure(R.string.vault_detail_history_load_failed) {
                    onAction(DetailUiAction.RetryActivityHistory)
                }
            }
        }
        if (content.revisionSource == DetailHistoryLoadState.FAILED) {
            item("revision-failed") {
                HistorySourceFailure(R.string.revision_load_failed) {
                    onAction(DetailUiAction.RetryRevisionHistory)
                }
            }
        }
        if (content.isEmptyResult) {
            item("empty") {
                Text(
                    text = stringResource(R.string.revision_history_empty),
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(content.items, key = { it.id }) { item ->
            val linkedRevision = item.revision as? DetailHistoryRevisionReference.Linked
            val title =
                when (val description = item.description) {
                    is DetailHistoryDescriptionUiState.Label -> {
                        stringResource(description.labelRes)
                    }

                    is DetailHistoryDescriptionUiState.RevisionEvent -> {
                        stringResource(
                            R.string.vault_detail_activity_revision_format,
                            stringResource(description.labelRes),
                            description.version,
                        )
                    }

                    is DetailHistoryDescriptionUiState.Snapshot -> {
                        stringResource(R.string.revision_version_format, description.version)
                    }
                }
            val subtitle = formatter.format(Date(item.createdAtMs))
            Surface(
                onClick = {
                    linkedRevision?.let { onAction(DetailUiAction.SelectRevision(it.revisionId)) }
                },
                enabled = linkedRevision != null,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(historyIcon(item.icon), contentDescription = null)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        (item.description as? DetailHistoryDescriptionUiState.Snapshot)?.let { snapshot ->
                            Text(
                                stringResource(snapshot.changeLabelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        when (val affectedFields = item.affectedFields) {
                            DetailHistoryAffectedFieldsUiState.None -> {
                                Unit
                            }

                            is DetailHistoryAffectedFieldsUiState.Labels -> {
                                Text(
                                    affectedFields.items.map { it.resolveText() }.joinToString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (linkedRevision != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    selected: DetailHistoryFilter,
    filter: DetailHistoryFilter,
    labelRes: Int,
    onAction: (DetailUiAction) -> Unit,
) {
    FilterChip(
        selected = selected == filter,
        onClick = { onAction(DetailUiAction.HistoryFilterChanged(filter)) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun HistorySourceFailure(
    messageRes: Int,
    onRetry: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(messageRes),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.vault_detail_history_retry)) }
    }
}

private fun historyIcon(icon: DetailHistoryIcon) =
    when (icon) {
        DetailHistoryIcon.CREATED -> Icons.Outlined.AddCircleOutline
        DetailHistoryIcon.EDITED -> Icons.Outlined.Edit
        DetailHistoryIcon.VERSION_RESTORED -> Icons.Outlined.Restore
        DetailHistoryIcon.DELETED -> Icons.Outlined.DeleteOutline
        DetailHistoryIcon.TRASH_RESTORED -> Icons.Outlined.Unarchive
        DetailHistoryIcon.VIEWED -> Icons.Outlined.Visibility
        DetailHistoryIcon.REVEALED -> Icons.Outlined.LockOpen
        DetailHistoryIcon.COPIED -> Icons.Outlined.ContentCopy
        DetailHistoryIcon.AUTOFILLED -> Icons.Outlined.AutoAwesome
        DetailHistoryIcon.EXPORTED -> Icons.Outlined.FileDownload
        DetailHistoryIcon.IMPORTED -> Icons.Outlined.FileUpload
        DetailHistoryIcon.SNAPSHOT -> Icons.Outlined.History
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RevisionComparison(
    content: DetailRevisionComparisonUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val restoreInProgress = content.restoreInProgress
            IconButton(
                enabled = !restoreInProgress,
                onClick = { onAction(DetailUiAction.RevisionBack) },
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                text = stringResource(R.string.revision_comparison_title),
                style = passlySheetTitleStyle(),
                modifier = Modifier.weight(1f),
            )
        }
        when (content) {
            DetailRevisionComparisonUiState.Loading -> {
                Loading()
            }

            DetailRevisionComparisonUiState.Failed -> {
                Text(
                    text = stringResource(R.string.revision_load_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )
            }

            is DetailRevisionComparisonUiState.Ready -> {
                when (val comparison = content.content) {
                    DetailRevisionComparisonContentUiState.NoChanges -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TaskAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    text = stringResource(R.string.revision_no_changes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }

                    is DetailRevisionComparisonContentUiState.Changes -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false).testTag("revision-comparison-list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(comparison.differences, key = { it.field.toString() }) { difference ->
                                RevisionDifferenceRow(
                                    difference,
                                    onAction,
                                    revealEnabled = content.restore == DetailRevisionRestorePhase.Idle,
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = { onAction(DetailUiAction.RequestRevisionRestore) },
                    enabled = content.restoreEnabled,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                ) {
                    if (content.restore == DetailRevisionRestorePhase.Restoring) {
                        LoadingIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(stringResource(R.string.revision_restore_action))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Loading() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.Center,
    ) { LoadingIndicator(modifier = Modifier.size(32.dp)) }
}
