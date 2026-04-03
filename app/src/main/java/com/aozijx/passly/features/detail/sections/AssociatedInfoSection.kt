package com.aozijx.passly.features.detail.sections

import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.MainViewModel
import com.aozijx.passly.R
import com.aozijx.passly.features.detail.components.InfoGroupCard
import com.aozijx.passly.core.designsystem.state.VaultEditState
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.media.FaviconUtils
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.features.vault.VaultViewModel
import kotlinx.coroutines.launch

@Composable
fun AssociatedInfoSection(
    entry: VaultEntry,
    editState: VaultEditState,
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    onEntryUpdated: (VaultEntry) -> Unit = vaultViewModel::updateVaultEntry,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloadingFavicon by remember { mutableStateOf(false) }
    var localDomain by remember(entry.associatedDomain) { mutableStateOf(entry.associatedDomain ?: "") }

    val domainAndIconLabel = stringResource(R.string.vault_detail_domain_and_icon)
    val associatedDomainLabel = stringResource(R.string.vault_detail_associated_domain)
    val associatedPackageLabel = stringResource(R.string.vault_detail_associated_package)
    val domainLabel = stringResource(R.string.vault_detail_domain_label)
    val domainPlaceholder = stringResource(R.string.vault_detail_domain_placeholder)
    val notSet = stringResource(R.string.vault_detail_not_set)
    val copied = stringResource(R.string.vault_detail_copied)
    val downloadIcon = stringResource(R.string.vault_detail_download_icon)
    val iconDownloadSuccess = stringResource(R.string.vault_detail_icon_download_success)
    val iconDownloadFailed = stringResource(R.string.vault_detail_icon_download_failed)
    val iconDownloadNetworkError = stringResource(R.string.vault_detail_icon_download_network_error)
    val iconDownloadDecodeError = stringResource(R.string.vault_detail_icon_download_decode_error)
    val iconDownloadSaveError = stringResource(R.string.vault_detail_icon_download_save_error)

    val isTotpEntry = entry.entryType == 1

    if (isTotpEntry) {
        InfoGroupCard(title = domainAndIconLabel) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!editState.isEditingDomain) {
                            Modifier.combinedClickable(
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    localDomain = entry.associatedDomain ?: ""
                                    editState.editedDomain = localDomain
                                    editState.isEditingDomain = true
                                },
                                onClick = { }
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (editState.isEditingDomain) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = localDomain,
                            onValueChange = {
                                localDomain = it
                                editState.editedDomain = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(domainLabel) },
                            placeholder = { Text(domainPlaceholder) },
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = {
                                    if (localDomain.isNotBlank()) {
                                        scope.launch {
                                            isDownloadingFavicon = true
                                            val outcome = FaviconUtils.downloadAndSaveFavicon(localDomain, context)
                                            when (outcome.result) {
                                                FaviconUtils.DownloadResult.SUCCESS -> {
                                                    onEntryUpdated(
                                                        entry.copy(
                                                            associatedDomain = localDomain.ifBlank { null },
                                                            iconName = null,
                                                            iconCustomPath = outcome.filePath,
                                                            totpSecret = entry.totpSecret,
                                                            totpPeriod = entry.totpPeriod,
                                                            totpDigits = entry.totpDigits,
                                                            totpAlgorithm = entry.totpAlgorithm
                                                        )
                                                    )
                                                    Toast.makeText(context, iconDownloadSuccess, Toast.LENGTH_SHORT).show()
                                                }
                                                FaviconUtils.DownloadResult.NETWORK_ERROR -> {
                                                    Toast.makeText(context, iconDownloadNetworkError, Toast.LENGTH_SHORT).show()
                                                }
                                                FaviconUtils.DownloadResult.DECODE_ERROR -> {
                                                    Toast.makeText(context, iconDownloadDecodeError, Toast.LENGTH_SHORT).show()
                                                }
                                                FaviconUtils.DownloadResult.SAVE_ERROR -> {
                                                    Toast.makeText(context, iconDownloadSaveError, Toast.LENGTH_SHORT).show()
                                                }
                                                FaviconUtils.DownloadResult.EMPTY_INPUT -> {
                                                    Toast.makeText(context, iconDownloadFailed, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            isDownloadingFavicon = false
                                        }
                                    }
                                },
                                enabled = localDomain.isNotBlank() && !isDownloadingFavicon
                            ) {
                                if (isDownloadingFavicon) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(downloadIcon)
                            }
                        }

                        TextButton(
                            onClick = {
                                val updatedEntry = editState.applyAssociatedOnly(entry)
                                onEntryUpdated(updatedEntry)
                                editState.isEditingDomain = false
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_save))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(domainLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = entry.associatedDomain ?: notSet,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        }
                        if (entry.associatedDomain != null) {
                            IconButton(onClick = {
                                ClipboardUtils.copy(context, entry.associatedDomain)
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfoGroupCard(title = associatedDomainLabel) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (editState.isEditingDomain) {
                    OutlinedTextField(
                        value = localDomain,
                        onValueChange = {
                            localDomain = it
                            editState.editedDomain = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(domainLabel) },
                        placeholder = { Text(domainPlaceholder) },
                        singleLine = true
                    )
                    TextButton(
                        onClick = {
                            val updatedEntry = editState.applyAssociatedOnly(entry)
                            onEntryUpdated(updatedEntry)
                            editState.isEditingDomain = false
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_save))
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    localDomain = entry.associatedDomain ?: ""
                                    editState.editedDomain = localDomain
                                    editState.isEditingDomain = true
                                },
                                onClick = { }
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(domainLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = entry.associatedDomain ?: notSet,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        }
                        if (entry.associatedDomain != null) {
                            IconButton(onClick = {
                                ClipboardUtils.copy(context, entry.associatedDomain)
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        InfoGroupCard(title = associatedPackageLabel) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (editState.isEditingPackage) {
                    OutlinedTextField(
                        value = editState.editedPackage,
                        onValueChange = { editState.editedPackage = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(associatedPackageLabel) },
                        singleLine = true
                    )
                    TextButton(
                        onClick = {
                            val updatedEntry = editState.applyAssociatedOnly(entry)
                            onEntryUpdated(updatedEntry)
                            editState.isEditingPackage = false
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_save))
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    editState.editedPackage = entry.associatedAppPackage ?: ""
                                    editState.isEditingPackage = true
                                },
                                onClick = { }
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(associatedPackageLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                entry.associatedAppPackage ?: notSet,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        }
                        if (entry.associatedAppPackage != null) {
                            IconButton(onClick = {
                                ClipboardUtils.copy(context, entry.associatedAppPackage)
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}



