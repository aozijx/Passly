package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.widgets.SwipeActionSpec
import com.aozijx.passly.core.ui.components.widgets.SwipeActionContainer
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordUnlinkUiState
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordUnlinkTargetUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedEntrySectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedPasswordSectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedSectionsUiModel
import com.aozijx.passly.presentation.shared.entry.labelRes

@Composable
fun RelatedEntriesSection(
    state: DetailRelatedSectionsUiModel,
    unlinkState: RelatedPasswordUnlinkUiState,
    onAction: (DetailUiAction) -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    when (val entries = state.entries) {
        DetailRelatedEntrySectionUiState.Hidden -> Unit
        is DetailRelatedEntrySectionUiState.Items -> {
            InfoGroupCard(title = stringResource(R.string.related_entries)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    entries.entries.forEachIndexed { index, entry ->
                        ListItem(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .testTag("related-entry-row-${entry.id}")
                                    .clickable { onOpenEntry(entry.id) }
                                    .padding(horizontal = 4.dp),
                            trailingContent = {
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            },
                            supportingContent = { Text(stringResource(entry.entryType.labelRes)) },
                            colors = ListItemDefaults.colors(),
                            content = { Text(entry.title) },
                        )
                        if (index < entries.entries.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }

    when (val passwords = state.passwords) {
        DetailRelatedPasswordSectionUiState.Hidden -> Unit
        DetailRelatedPasswordSectionUiState.EmptyCanBind -> {
            InfoGroupCard(
                title = stringResource(R.string.vault_detail_related_passwords_title),
                titleTrailing = { BindPasswordButton(onClick = { onAction(DetailUiAction.OpenPasswordBinding) }) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.vault_detail_related_passwords_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onAction(DetailUiAction.OpenPasswordBinding) }) {
                        Text(stringResource(R.string.vault_detail_bind_password))
                    }
                }
            }
        }

        is DetailRelatedPasswordSectionUiState.Items -> {
            InfoGroupCard(
                title = stringResource(R.string.vault_detail_related_passwords_title),
                titleTrailing =
                    if (passwords.canBind) {
                        { BindPasswordButton(onClick = { onAction(DetailUiAction.OpenPasswordBinding) }) }
                    } else {
                        null
                    },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    passwords.entries.forEachIndexed { index, related ->
                        val removing =
                            unlinkState is RelatedPasswordUnlinkUiState.Removing &&
                                unlinkState.target.targetEntryId == related.targetEntryId
                        val requestUnlink = {
                            onAction(DetailUiAction.RequestPasswordUnlink(related.targetEntryId, related.linkId))
                        }
                        val unlinkAccessibilityLabel =
                            stringResource(R.string.vault_detail_unlink_accessibility_action)
                        val row = @Composable {
                            ListItem(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .testTag("related-password-row-${related.targetEntryId}")
                                        .semantics {
                                            if (!removing) {
                                                customActions =
                                                    listOf(
                                                        CustomAccessibilityAction(
                                                            label = unlinkAccessibilityLabel,
                                                            action = {
                                                                requestUnlink()
                                                                true
                                                            },
                                                        ),
                                                    )
                                            }
                                        }.clickable { onOpenEntry(related.targetEntryId) }
                                        .padding(horizontal = 4.dp),
                                trailingContent = {
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                },
                                supportingContent = {
                                    related.username?.let { Text(it) }
                                },
                                colors = ListItemDefaults.colors(),
                                content = { Text(related.title) },
                            )
                        }
                        SwipeActionContainer(
                            leftAction =
                                if (removing) {
                                    null
                                } else {
                                    SwipeActionSpec(
                                        icon = Icons.Default.LinkOff,
                                        backgroundColor = MaterialTheme.colorScheme.errorContainer,
                                        iconTint = MaterialTheme.colorScheme.onErrorContainer,
                                        onAction = requestUnlink,
                                    )
                                },
                            rightAction = null,
                            enabled = !removing,
                            content = row,
                        )
                        if (index < passwords.entries.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }

    UnlinkConfirmationDialog(state = unlinkState, onAction = onAction)
}

@Composable
private fun BindPasswordButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = stringResource(R.string.vault_detail_bind_password),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UnlinkConfirmationDialog(
    state: RelatedPasswordUnlinkUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    when (state) {
        RelatedPasswordUnlinkUiState.Closed -> Unit
        is RelatedPasswordUnlinkUiState.AwaitingConfirmation -> {
            UnlinkConfirmationDialogContent(
                target = state.target,
                confirmTextRes = R.string.vault_detail_unlink_confirm,
                confirmEnabled = true,
                onDismiss = { onAction(DetailUiAction.DismissPasswordUnlink) },
                statusContent = {},
                onConfirm = { onAction(DetailUiAction.ConfirmPasswordUnlink) },
            )
        }

        is RelatedPasswordUnlinkUiState.Removing -> {
            UnlinkConfirmationDialogContent(
                target = state.target,
                confirmTextRes = R.string.vault_detail_unlink_removing,
                confirmEnabled = false,
                onDismiss = null,
                statusContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LoadingIndicator(modifier = Modifier.size(24.dp))
                        Text(stringResource(R.string.vault_detail_unlink_removing))
                    }
                },
                onConfirm = { onAction(DetailUiAction.ConfirmPasswordUnlink) },
            )
        }

        is RelatedPasswordUnlinkUiState.Failed -> {
            UnlinkConfirmationDialogContent(
                target = state.target,
                confirmTextRes = R.string.vault_detail_unlink_confirm,
                confirmEnabled = true,
                onDismiss = { onAction(DetailUiAction.DismissPasswordUnlink) },
                statusContent = {
                    Text(
                        stringResource(R.string.vault_detail_unlink_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onConfirm = { onAction(DetailUiAction.ConfirmPasswordUnlink) },
            )
        }
    }
}

@Composable
private fun UnlinkConfirmationDialogContent(
    target: RelatedPasswordUnlinkTargetUiModel,
    confirmTextRes: Int,
    confirmEnabled: Boolean,
    onDismiss: (() -> Unit)?,
    statusContent: @Composable ColumnScope.() -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = { Text(stringResource(R.string.vault_detail_unlink_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.vault_detail_unlink_message, target.targetTitle))
                statusContent()
            }
        },
        confirmButton = {
            TextButton(
                enabled = confirmEnabled,
                onClick = onConfirm,
            ) {
                Text(stringResource(confirmTextRes))
            }
        },
        dismissButton = {
            TextButton(
                enabled = confirmEnabled,
                onClick = { onDismiss?.invoke() },
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
