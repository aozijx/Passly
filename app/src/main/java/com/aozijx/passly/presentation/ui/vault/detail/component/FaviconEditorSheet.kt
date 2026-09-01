package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.VaultIconColorToken
import com.aozijx.passly.presentation.ui.shared.components.VaultIcons
import com.aozijx.passly.presentation.ui.shared.components.iconColorForStorageToken
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconEditorTabUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconProcessingErrorUiModel
import coil.compose.AsyncImage
import com.aozijx.passly.presentation.ui.shared.media.toLocalIconImageModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaviconEditorSheet(
    state: DetailFaviconEditorUiModel,
    isSaving: Boolean,
    onTabSelected: (FaviconEditorTabUiModel) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSourceSelected: (FaviconDraftSourceUiModel) -> Unit,
    onUploadRequested: () -> Unit,
    onImageUrlChanged: (String) -> Unit,
    onDownloadRequested: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.vault_detail_favicon_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            FaviconPreview(state.source)
            PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                FaviconEditorTabUiModel.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    FaviconEditorTabUiModel.ICON_LIBRARY -> stringResource(R.string.vault_detail_favicon_library)
                                    FaviconEditorTabUiModel.UPLOAD -> stringResource(R.string.vault_detail_favicon_upload)
                                    FaviconEditorTabUiModel.IMAGE_URL -> stringResource(R.string.vault_detail_favicon_image_url)
                                },
                            )
                        },
                    )
                }
            }
            when (state.selectedTab) {
                FaviconEditorTabUiModel.ICON_LIBRARY -> FaviconIconLibrary(
                    state = state,
                    onSearchChanged = onSearchChanged,
                    onSourceSelected = onSourceSelected,
                )
                FaviconEditorTabUiModel.UPLOAD -> Button(
                    onClick = onUploadRequested,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Text(
                        text = stringResource(R.string.vault_detail_favicon_choose_image),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                FaviconEditorTabUiModel.IMAGE_URL -> Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.imageUrl,
                        onValueChange = onImageUrlChanged,
                        enabled = !state.processing,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.vault_detail_favicon_image_url)) },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    )
                    Button(
                        onClick = onDownloadRequested,
                        enabled = state.imageUrl.trim().startsWith("https://") && !state.processing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Text(
                            text = stringResource(R.string.vault_detail_favicon_download),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    state.processingError?.let { error ->
                        Text(
                            text = stringResource(error.messageRes()),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            TextButton(
                onClick = { onSourceSelected(FaviconDraftSourceUiModel.InferredDefault) },
                enabled = !isSaving,
            ) {
                Text(stringResource(R.string.vault_detail_favicon_restore_default))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = onSave,
                    enabled = state.dirty && !isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
    if (state.confirmDiscard) {
        AlertDialog(
            onDismissRequest = onKeepEditing,
            title = { Text(stringResource(R.string.vault_detail_favicon_discard_title)) },
            text = { Text(stringResource(R.string.vault_detail_favicon_discard_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text(stringResource(R.string.vault_detail_favicon_discard_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onKeepEditing) { Text(stringResource(R.string.vault_detail_favicon_keep_editing)) }
            },
        )
    }
}

private fun FaviconProcessingErrorUiModel.messageRes(): Int = when (this) {
    FaviconProcessingErrorUiModel.INVALID_URL -> R.string.vault_detail_favicon_error_invalid_url
    FaviconProcessingErrorUiModel.URL_NOT_ALLOWED -> R.string.vault_detail_favicon_error_url_not_allowed
    FaviconProcessingErrorUiModel.DOWNLOAD_FAILED -> R.string.vault_detail_favicon_error_download_failed
    FaviconProcessingErrorUiModel.NOT_IMAGE -> R.string.vault_detail_favicon_error_not_image
    FaviconProcessingErrorUiModel.IMAGE_TOO_LARGE -> R.string.vault_detail_favicon_error_too_large
    FaviconProcessingErrorUiModel.INVALID_IMAGE -> R.string.vault_detail_favicon_error_invalid_image
    FaviconProcessingErrorUiModel.SAVE_FAILED -> R.string.vault_detail_favicon_error_save_failed
}

@Composable
private fun FaviconPreview(source: FaviconDraftSourceUiModel) {
    val text = when (source) {
        FaviconDraftSourceUiModel.InferredDefault -> stringResource(R.string.vault_detail_favicon_default)
        is FaviconDraftSourceUiModel.BuiltIn -> VaultIcons.findDefinition(source.key)
            ?.let { stringResource(it.labelRes) }
            ?: stringResource(R.string.vault_detail_favicon_built_in)
        is FaviconDraftSourceUiModel.PrivateImage -> stringResource(R.string.vault_detail_favicon_custom_image)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (source is FaviconDraftSourceUiModel.PrivateImage) {
                AsyncImage(
                    model = toLocalIconImageModel(source.stagedPath),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FaviconIconLibrary(
    state: DetailFaviconEditorUiModel,
    onSearchChanged: (String) -> Unit,
    onSourceSelected: (FaviconDraftSourceUiModel) -> Unit,
) {
    val selected = state.source as? FaviconDraftSourceUiModel.BuiltIn
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.search)) },
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            modifier = Modifier.heightIn(max = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(VaultIcons.search(state.searchQuery), key = { it.key }) { definition ->
                val isSelected = selected?.let { VaultIcons.findDefinition(it.key)?.key } == definition.key
                Surface(
                    onClick = {
                        onSourceSelected(
                            FaviconDraftSourceUiModel.BuiltIn(definition.key, selected?.colorToken),
                        )
                    },
                    modifier = Modifier.size(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = definition.imageVector,
                            contentDescription = stringResource(definition.labelRes),
                            tint = iconColorForStorageToken(selected?.colorToken, MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.BottomEnd).size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val tokens = listOf<String?>(null) + VaultIconColorToken.entries.map { it.storageValue }
            tokens.forEach { token ->
                val color = iconColorForStorageToken(token, MaterialTheme.colorScheme.onSurfaceVariant)
                FilterChip(
                    selected = selected?.colorToken == token,
                    onClick = {
                        selected?.let {
                            val stableKey = VaultIcons.findDefinition(it.key)?.key ?: it.key
                            onSourceSelected(it.copy(key = stableKey, colorToken = token))
                        }
                    },
                    label = { Text(if (token == null) "A" else "") },
                    leadingIcon = {
                        Surface(modifier = Modifier.size(18.dp), shape = CircleShape, color = color) {}
                    },
                )
            }
        }
    }
}
