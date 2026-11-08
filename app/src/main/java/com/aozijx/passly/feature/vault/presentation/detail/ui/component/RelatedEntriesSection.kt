package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRelatedEntrySectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRelatedPasswordSectionUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailRelatedSectionsUiModel
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
                            (unlinkState as? RelatedPasswordUnlinkUiState.Confirming)
                                ?.takeIf { it.phase == RelatedPasswordUnlinkUiState.Phase.Removing }
                                ?.targetEntryId == related.targetEntryId
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

@Composable
private fun UnlinkConfirmationDialog(
    state: RelatedPasswordUnlinkUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val confirmation = state as? RelatedPasswordUnlinkUiState.Confirming ?: return
    val removing = confirmation.phase == RelatedPasswordUnlinkUiState.Phase.Removing
    val failed = confirmation.phase == RelatedPasswordUnlinkUiState.Phase.Failed
    AlertDialog(
        onDismissRequest = {
            if (!removing) onAction(DetailUiAction.DismissPasswordUnlink)
        },
        title = { Text(stringResource(R.string.vault_detail_unlink_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.vault_detail_unlink_message, confirmation.targetTitle))
                if (failed) Text(stringResource(R.string.vault_detail_unlink_failed), color = MaterialTheme.colorScheme.error)
                if (removing) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.vault_detail_unlink_removing))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !removing,
                onClick = { onAction(DetailUiAction.ConfirmPasswordUnlink) },
            ) {
                Text(stringResource(if (removing) R.string.vault_detail_unlink_removing else R.string.vault_detail_unlink_confirm))
            }
        },
        dismissButton = {
            TextButton(
                enabled = !removing,
                onClick = { onAction(DetailUiAction.DismissPasswordUnlink) },
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
