package com.aozijx.passly.feature.detail.sections

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.media.FaviconUtils
import com.aozijx.passly.core.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.core.ui.components.rememberAppIcon
import com.aozijx.passly.core.ui.components.rememberAppMetadata
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import com.aozijx.passly.domain.notice.model.NoticeCode
import com.aozijx.passly.domain.notice.model.newAppNotice
import com.aozijx.passly.domain.notice.port.AppNoticePublisher
import com.aozijx.passly.feature.detail.components.InfoGroupCard
import com.aozijx.passly.feature.detail.internal.EntryEditState
import kotlinx.coroutines.launch

@Composable
internal fun LoginDomainIconCard(
    entry: VaultEntry,
    editState: EntryEditState,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noticePublisher = LocalAppNoticePublisher.current
    var downloading by remember { mutableStateOf(false) }
    var domainInput by remember(entry.associatedDomain) {
        mutableStateOf(TextFieldValue(entry.associatedDomain.orEmpty()))
    }
    val notSet = stringResource(R.string.not_set)
    val associatedPackages = entry.website?.packageNames.orEmpty().sorted()

    InfoGroupCard(title = stringResource(R.string.vault_detail_domain_and_icon)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (editState.isEditingDomain) Modifier
                    else Modifier.combinedClickable(
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            domainInput = TextFieldValue(entry.associatedDomain.orEmpty())
                            editState.editedDomain = domainInput.text
                            editState.isEditingDomain = true
                        },
                        onClick = {}
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (editState.isEditingDomain) {
                DomainEditor(
                    value = domainInput,
                    downloading = downloading,
                    onValueChange = {
                        domainInput = it
                        editState.editedDomain = it.text
                    },
                    onDownload = {
                        scope.launch {
                            downloading = true
                            downloadFavicon(
                                domain = domainInput.text.trim(),
                                entry = entry,
                                context = context,
                                noticePublisher = noticePublisher,
                                onUpdateVaultEntry = onUpdateVaultEntry,
                                onEntryUpdated = onEntryUpdated
                            )
                            downloading = false
                        }
                    },
                    onDone = {
                        focusManager.clearFocus()
                        val updated = editState.applyAssociatedOnly(entry)
                        onUpdateVaultEntry(updated)
                        onEntryUpdated(updated)
                        editState.isEditingDomain = false
                    }
                )
            } else {
                Text(
                    text = entry.associatedDomain ?: notSet,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            associatedPackages.forEach { packageName ->
                AssociatedPackageRow(packageName)
            }
        }
    }
}

@Composable
private fun AssociatedPackageRow(packageName: String) {
    val icon = rememberAppIcon(packageName)
    val metadata = rememberAppMetadata(packageName)
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
        Column(modifier = Modifier.weight(1f)) {
            metadata?.appName?.takeIf { it != packageName }?.let { appName ->
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
            value = value,
            onValueChange = onValueChange,
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

private suspend fun downloadFavicon(
    domain: String,
    entry: VaultEntry,
    context: android.content.Context,
    noticePublisher: AppNoticePublisher,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    if (domain.isBlank()) return
    val outcome = FaviconUtils.downloadAndSaveFavicon(domain, context)
    val code = when (outcome.result) {
        FaviconUtils.DownloadResult.SUCCESS -> {
            val updated = entry.copy(
                summary = entry.summary.copy(
                    website = (entry.summary.website ?: WebsiteInfo()).copy(
                        primaryUrl = domain
                    ),
                    icon = null
                )
            )
            onUpdateVaultEntry(updated)
            onEntryUpdated(updated)
            NoticeCode.ICON_DOWNLOAD_COMPLETED
        }
        FaviconUtils.DownloadResult.NETWORK_ERROR,
        FaviconUtils.DownloadResult.DECODE_ERROR,
        FaviconUtils.DownloadResult.SAVE_ERROR,
        FaviconUtils.DownloadResult.EMPTY_INPUT -> NoticeCode.ICON_DOWNLOAD_FAILED
    }
    noticePublisher.publish(newAppNotice(code))
}
