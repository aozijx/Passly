package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.AppPackagePickerBottomSheet
import androidx.compose.material3.OutlinedTextField
import com.aozijx.passly.presentation.ui.shared.components.rememberAppIcon
import com.aozijx.passly.presentation.ui.shared.components.rememberAppMetadata
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailAssociatedInfoUiModel

@Composable
fun AssociatedInfoSection(
    modifier: Modifier = Modifier,
    model: DetailAssociatedInfoUiModel,
    onDownloadFavicon: (String) -> Unit,
    onDomainEditStarted: () -> Unit,
    onDomainChanged: (String) -> Unit,
    onDomainSaved: (String) -> Unit,
    onPackageSelected: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    var showPackagePicker by remember { mutableStateOf(false) }
    var domainInput by remember(model.domain) {
        mutableStateOf(TextFieldValue(model.domain.orEmpty()))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AssociatedDomainCard(
            value = model.domain,
            editedValue = domainInput,
            editing = model.isEditingDomain,
            downloading = model.isFaviconDownloading,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                domainInput = TextFieldValue(model.domain.orEmpty())
                onDomainEditStarted()
            },
            onValueChange = {
                domainInput = it
                onDomainChanged(it.text)
            },
            onDownload = { onDownloadFavicon(domainInput.text.trim()) },
            onSave = {
                focusManager.clearFocus()
                onDomainSaved(domainInput.text)
            }
        )

        AssociatedAppsCard(
            packageNames = model.applicationIds,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showPackagePicker = true
            }
        )
    }

    if (showPackagePicker) {
        AppPackagePickerBottomSheet(
            onSelect = {
                onPackageSelected(it.packageName)
                showPackagePicker = false
            },
            onDismiss = { showPackagePicker = false }
        )
    }
}
@Composable
private fun AssociatedDomainCard(
    value: String?,
    editedValue: TextFieldValue,
    editing: Boolean,
    downloading: Boolean,
    onLongClick: () -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    onDownload: () -> Unit,
    onSave: () -> Unit
) {
    InfoGroupCard(title = stringResource(R.string.vault_detail_associated_domain)) {
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
                DomainEditor(
                    value = editedValue,
                    downloading = downloading,
                    onValueChange = onValueChange,
                    onDownload = onDownload,
                    onDone = onSave
                )
            } else {
                Text(
                    text = value ?: stringResource(R.string.not_set),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
@Composable
private fun AssociatedAppsCard(
    packageNames: List<String>,
    onLongClick: () -> Unit,
) {
    InfoGroupCard(title = stringResource(R.string.vault_detail_associated_package)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onLongClick = onLongClick, onClick = {})
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (packageNames.isEmpty()) {
                Text(
                    text = stringResource(R.string.not_set),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            } else {
                packageNames.forEach { packageName ->
                    AssociatedAppRow(packageName)
                }
            }
        }
    }
}

@Composable
private fun AssociatedAppRow(packageName: String) {
    val icon = rememberAppIcon(packageName)
    val metadata = rememberAppMetadata(packageName)
    val appName = metadata?.label?.takeIf { it.isNotBlank() } ?: packageName

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
        }
        Text(
            text = appName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DomainEditor(
    value: TextFieldValue,
    downloading: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    onDownload: () -> Unit,
    onDone: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value.text,
            onValueChange = { onValueChange(value.copy(text = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.vault_detail_domain_label)) },
            placeholder = { Text(stringResource(R.string.vault_detail_domain_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() })
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDownload,
                enabled = value.text.isNotBlank() && !downloading
            ) {
                if (downloading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_detail_download_icon))
            }
            TextButton(onClick = onDone) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.save))
            }
        }
    }
}

