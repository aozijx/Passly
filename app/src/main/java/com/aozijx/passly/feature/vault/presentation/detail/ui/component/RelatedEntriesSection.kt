package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.widgets.SwipeActionContainer
import com.aozijx.passly.core.ui.components.widgets.SwipeActionSpec
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordUnlinkPhase
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordUnlinkTargetUiModel
import com.aozijx.passly.feature.vault.presentation.detail.RelatedPasswordUnlinkUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedEntrySectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedPasswordSectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailRelatedSectionsUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.RelatedPasswordRowUnlinkUiState

@Composable
fun RelatedEntriesSection(
    state: DetailRelatedSectionsUiModel,
    unlinkState: RelatedPasswordUnlinkUiState,
    onAction: (DetailUiAction) -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    when (val entries = state.entries) {
        DetailRelatedEntrySectionUiState.Hidden -> {
            Unit
        }

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
                            supportingContent = { Text(stringResource(entry.entryTypeLabelRes)) },
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
        DetailRelatedPasswordSectionUiState.Hidden -> {
            Unit
        }

        DetailRelatedPasswordSectionUiState.EmptyCanBind -> {
            InfoGroupCard(
                title = stringResource(R.string.vault_detail_related_passwords_title),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onAction(DetailUiAction.OpenPasswordBinding) },
                                onLongClick = { onAction(DetailUiAction.OpenPasswordBinding) },
                            ).padding(horizontal = 16.dp, vertical = 14.dp),
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
                }
            }
        }

        is DetailRelatedPasswordSectionUiState.Items -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                InfoGroupHeader(
                    title = stringResource(R.string.vault_detail_related_passwords_title),
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    passwords.entries.forEach { related ->
                        val removing =
                            related.unlinkActionState == RelatedPasswordRowUnlinkUiState.Removing
                        val requestUnlink = {
                            onAction(
                                DetailUiAction.RequestPasswordUnlink(
                                    related.targetEntryId,
                                    related.linkId,
                                ),
                            )
                        }
                        val unlinkAccessibilityLabel =
                            stringResource(R.string.vault_detail_unlink_accessibility_action)
                        val row = @Composable {
                            val rowSemanticsModifier =
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
                                    }
                            val rowInteractionModifier =
                                if (passwords.canBind) {
                                    rowSemanticsModifier.combinedClickable(
                                        onClick = { onOpenEntry(related.targetEntryId) },
                                        onLongClick = {
                                            onAction(DetailUiAction.OpenPasswordBinding)
                                        },
                                    )
                                } else {
                                    rowSemanticsModifier.clickable {
                                        onOpenEntry(related.targetEntryId)
                                    }
                                }
                            ListItem(
                                modifier =
                                    rowInteractionModifier.padding(horizontal = 4.dp),
                                trailingContent = {
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                },
                                supportingContent = {
                                    Text(related.username)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
                            shape = MaterialTheme.shapes.large,
                            foregroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            enabled = !removing,
                            content = row,
                        )
                    }
                }
            }
        }
    }

    UnlinkConfirmationDialog(state = unlinkState, onAction = onAction)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UnlinkConfirmationDialog(
    state: RelatedPasswordUnlinkUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    when (state) {
        RelatedPasswordUnlinkUiState.Closed -> {
            Unit
        }

        is RelatedPasswordUnlinkUiState.Open -> {
            when (state.phase) {
                RelatedPasswordUnlinkPhase.AwaitingConfirmation -> {
                    UnlinkConfirmationDialogContent(
                        target = state.target,
                        confirmTextRes = R.string.vault_detail_unlink_confirm,
                        confirmEnabled = true,
                        onDismiss = { onAction(DetailUiAction.DismissPasswordUnlink) },
                        statusContent = {},
                        onConfirm = { onAction(DetailUiAction.ConfirmPasswordUnlink) },
                    )
                }

                RelatedPasswordUnlinkPhase.Removing -> {
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

                RelatedPasswordUnlinkPhase.Failed -> {
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
