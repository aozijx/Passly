package com.aozijx.passly.presentation.feature.vault.detail.ui.component

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
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailRevisionDestinationUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailRevisionDifferenceUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailRevisionSheetUiModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionHistorySheet(
    model: DetailRevisionSheetUiModel,
    onAction: (DetailUiAction) -> Unit,
) {
    if (!model.visible) return
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    ModalBottomSheet(
        onDismissRequest = { onAction(DetailUiAction.DismissRevisionHistory) },
        modifier = Modifier.fillMaxHeight(0.94f),
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        when (model.destination) {
            DetailRevisionDestinationUiModel.LIST -> RevisionList(model, onAction)
            DetailRevisionDestinationUiModel.COMPARISON -> RevisionComparison(model, onAction)
        }
    }
    if (model.confirmRestore) {
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
    model: DetailRevisionSheetUiModel,
    onAction: (DetailUiAction) -> Unit,
) {
    val formatter = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    Column(Modifier.fillMaxWidth().fillMaxHeight()) {
        Text(
            text = stringResource(R.string.revision_history_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        when {
            model.loading -> Loading()
            model.revisions.isEmpty() -> Text(
                text = stringResource(R.string.revision_history_empty),
                modifier = Modifier.padding(24.dp),
            )
            else -> LazyColumn {
                items(model.revisions, key = { it.id }) { revision ->
                    ListItem(
                        supportingContent = { Text(formatter.format(Date(revision.createdAtMs))) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingContent = null,
                        trailingContent = null,
                        overlineContent = null,
                        colors = ListItemDefaults.colors(),
                        elevation = ListItemDefaults.elevation(),
                    ) { Text(stringResource(R.string.revision_version_format, revision.version)) }
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

@Composable
private fun RevisionComparison(
    model: DetailRevisionSheetUiModel,
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
        when {
            model.loading -> Loading()
            model.failure != null -> Text(
                text = stringResource(R.string.revision_load_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(model.differences, key = { it.field.toString() }) { difference ->
                    RevisionDifferenceRow(difference, onAction)
                }
            }
        }
        Button(
            onClick = { onAction(DetailUiAction.RequestRevisionRestore) },
            enabled = !model.loading && !model.restoring && model.failure == null,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        ) {
            if (model.restoring) CircularProgressIndicator()
            else Text(stringResource(R.string.revision_restore_action))
        }
    }
}

@Composable
private fun RevisionDifferenceRow(
    difference: DetailRevisionDifferenceUiModel,
    onAction: (DetailUiAction) -> Unit,
) {
    val sensitive = difference.field as? RevisionFieldId.Sensitive
    ListItem(
        supportingContent = {
            if (sensitive == null) {
                Text("${difference.before.orEmpty()} → ${difference.after.orEmpty()}")
            } else {
                val revealed = difference.revealedValue?.useChars(::String)
                Text(revealed ?: stringResource(R.string.revision_sensitive_masked))
            }
        },
        trailingContent = sensitive?.let { field ->
            @Composable {
                TextButton(onClick = { onAction(DetailUiAction.RevealRevisionField(field.key)) }) {
                    Text(stringResource(R.string.revision_reveal_action))
                }
            }
        },
        leadingContent = null,
        overlineContent = null,
        colors = ListItemDefaults.colors(),
        elevation = ListItemDefaults.elevation(),
    ) { Text(fieldLabel(difference.field)) }
}

@Composable
private fun fieldLabel(field: RevisionFieldId): String = when (field) {
    RevisionFieldId.Title -> stringResource(R.string.revision_field_title)
    RevisionFieldId.Username -> stringResource(R.string.vault_detail_username)
    RevisionFieldId.PrimaryUrl -> stringResource(R.string.revision_field_primary_url)
    RevisionFieldId.Domains -> stringResource(R.string.revision_field_domains)
    RevisionFieldId.ApplicationIds -> stringResource(R.string.revision_field_applications)
    RevisionFieldId.Icon -> stringResource(R.string.revision_field_icon)
    RevisionFieldId.Favorite -> stringResource(R.string.revision_field_favorite)
    RevisionFieldId.Tags -> stringResource(R.string.vault_detail_tags_title)
    RevisionFieldId.ExpiresAt -> stringResource(R.string.revision_field_expiration)
    RevisionFieldId.Credential -> stringResource(R.string.revision_field_credential)
    RevisionFieldId.Notes -> stringResource(R.string.revision_field_notes)
    RevisionFieldId.CustomFields -> stringResource(R.string.revision_field_custom_fields)
    RevisionFieldId.Attachments -> stringResource(R.string.revision_field_attachments)
    RevisionFieldId.Links -> stringResource(R.string.revision_field_links)
    is RevisionFieldId.Sensitive -> when (field.key) {
        com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.PASSWORD ->
            stringResource(R.string.password_label)
        com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.CARD_NUMBER ->
            stringResource(R.string.card_number)
        com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.CARD_CVV ->
            stringResource(R.string.card_cvv)
        com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.CARD_PAYMENT_PIN ->
            stringResource(R.string.payment_pin)
        com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.IDENTITY_NUMBER ->
            stringResource(R.string.id_number)
        com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.SEED_PHRASE ->
            stringResource(R.string.seed_phrase)
        com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.RECOVERY_CODES ->
            stringResource(R.string.recovery_code_label)
        com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.SSH_PRIVATE_KEY ->
            stringResource(R.string.ssh_private_key)
        com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.SSH_PASSPHRASE ->
            stringResource(R.string.passphrase)
        com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE ->
            stringResource(R.string.passkey_data)
        com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.OTP_SECRET ->
            stringResource(R.string.vault_detail_totp_label)
    }
}

@Composable
private fun Loading() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.Center,
    ) { CircularProgressIndicator() }
}
