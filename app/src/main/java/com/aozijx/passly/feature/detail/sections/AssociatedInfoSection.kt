package com.aozijx.passly.feature.detail.sections

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.AppPackagePickerDialog
import com.aozijx.passly.core.ui.components.PasslyOutlinedTextField
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.feature.detail.components.InfoGroupCard
import com.aozijx.passly.feature.detail.internal.EntryEditState

@Composable
fun AssociatedInfoSection(
    modifier: Modifier = Modifier,
    entry: VaultEntry,
    editState: EntryEditState,
    isFaviconDownloading: Boolean,
    onDownloadFavicon: (String) -> Unit,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    if (entry.entryType == EntryType.LOGIN) {
        LoginDomainIconCard(
            entry = entry,
            editState = editState,
            isFaviconDownloading = isFaviconDownloading,
            onDownloadFavicon = onDownloadFavicon,
            onEntryUpdated = onEntryUpdated
        )
        return
    }

    val haptic = LocalHapticFeedback.current
    var localDomain by remember(entry.associatedDomain) {
        mutableStateOf(entry.associatedDomain.orEmpty())
    }
    var showPackagePicker by remember { mutableStateOf(false) }
    val notSet = stringResource(R.string.not_set)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EditableAssociatedCard(
            title = stringResource(R.string.vault_detail_associated_domain),
            value = entry.associatedDomain,
            editedValue = localDomain,
            editing = editState.isEditingDomain,
            placeholder = stringResource(R.string.vault_detail_domain_placeholder),
            notSet = notSet,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                localDomain = entry.associatedDomain.orEmpty()
                editState.editedDomain = localDomain
                editState.isEditingDomain = true
            },
            onValueChange = {
                localDomain = it
                editState.editedDomain = it
            },
            onSave = {
                saveAssociated(entry, editState, onEntryUpdated)
                editState.isEditingDomain = false
            }
        )
        EditableAssociatedCard(
            title = stringResource(R.string.vault_detail_associated_package),
            value = entry.associatedAppPackage,
            editedValue = editState.editedPackage,
            editing = editState.isEditingPackage,
            notSet = notSet,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                editState.editedPackage = entry.associatedAppPackage.orEmpty()
                editState.isEditingPackage = true
            },
            onValueChange = { editState.editedPackage = it },
            onSave = {
                saveAssociated(entry, editState, onEntryUpdated)
                editState.isEditingPackage = false
            },
            onPickPackage = { showPackagePicker = true }
        )
    }

    if (showPackagePicker) {
        AppPackagePickerDialog(
            onSelect = {
                editState.editedPackage = it.packageName
                saveAssociated(entry, editState, onEntryUpdated)
                editState.isEditingPackage = false
                showPackagePicker = false
            },
            onDismiss = { showPackagePicker = false }
        )
    }
}

@Composable
private fun EditableAssociatedCard(
    title: String,
    value: String?,
    editedValue: String,
    editing: Boolean,
    notSet: String,
    placeholder: String? = null,
    onLongClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onPickPackage: (() -> Unit)? = null
) {
    InfoGroupCard(title = title) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (editing) Modifier
                    else Modifier.combinedClickable(onLongClick = onLongClick, onClick = {})
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (editing) {
                PasslyOutlinedTextField(
                    value = editedValue,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = title,
                    placeholder = placeholder?.let { { Text(it) } },
                    singleLine = true
                )
                onPickPackage?.let { pick ->
                    TextButton(onClick = pick) {
                        Icon(Icons.Default.Apps, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.app_package_picker_action))
                    }
                }
                TextButton(onClick = onSave, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.save))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value ?: notSet,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun saveAssociated(
    entry: VaultEntry,
    editState: EntryEditState,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    val updated = editState.applyAssociatedOnly(entry)
    onEntryUpdated(updated)
}
