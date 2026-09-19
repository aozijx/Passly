package com.aozijx.passly.presentation.feature.vault.detail.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconEditorTabUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconProcessingErrorUiModel
import coil.compose.AsyncImage
import com.aozijx.passly.presentation.ui.shared.media.toLocalIconImageModel
import kotlinx.coroutines.launch

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
    val dismissAllowed by rememberUpdatedState(!isSaving && !state.processing)
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        confirmValueChange = { target -> target != SheetValue.Hidden || dismissAllowed },
    )
    val scope = rememberCoroutineScope()
    val keepEditing: () -> Unit = {
        onKeepEditing()
        scope.launch { sheetState.show() }
        Unit
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FaviconEditorHeader(state.source)
                PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                    FaviconEditorTabUiModel.entries.forEach { tab ->
                        Tab(
                            selected = state.selectedTab == tab,
                            onClick = { onTabSelected(tab) },
                            enabled = dismissAllowed,
                            text = {
                                Text(
                                    when (tab) {
                                        FaviconEditorTabUiModel.ICON_LIBRARY -> stringResource(R.string.vault_detail_favicon_library)
                                        FaviconEditorTabUiModel.CUSTOM_IMAGE -> stringResource(R.string.vault_detail_favicon_custom_image)
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
                        modifier = Modifier.weight(1f),
                    )
                    FaviconEditorTabUiModel.CUSTOM_IMAGE -> FaviconCustomImage(
                        state = state,
                        onUploadRequested = onUploadRequested,
                        onImageUrlChanged = onImageUrlChanged,
                        onDownloadRequested = onDownloadRequested,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            state.processingError?.let { error ->
                Text(
                    text = stringResource(error.messageRes()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { onSourceSelected(FaviconDraftSourceUiModel.InferredDefault) },
                    enabled = dismissAllowed,
                ) {
                    Text(stringResource(R.string.vault_detail_favicon_restore_default))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss, enabled = dismissAllowed) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.size(4.dp))
                Button(
                    onClick = onSave,
                    enabled = state.dirty && dismissAllowed,
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
            onDismissRequest = keepEditing,
            title = { Text(stringResource(R.string.vault_detail_favicon_discard_title)) },
            text = { Text(stringResource(R.string.vault_detail_favicon_discard_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text(stringResource(R.string.vault_detail_favicon_discard_action))
                }
            },
            dismissButton = {
                TextButton(onClick = keepEditing) { Text(stringResource(R.string.vault_detail_favicon_keep_editing)) }
            },
        )
    }
}

@Composable
private fun FaviconCustomImage(
    state: DetailFaviconEditorUiModel,
    onUploadRequested: () -> Unit,
    onImageUrlChanged: (String) -> Unit,
    onDownloadRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilledTonalButton(
            onClick = onUploadRequested,
            enabled = !state.processing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Text(
                text = stringResource(R.string.vault_detail_favicon_choose_image),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        HorizontalDivider()
        OutlinedTextField(
            value = state.imageUrl,
            onValueChange = onImageUrlChanged,
            enabled = !state.processing,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.vault_detail_favicon_image_url)) },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            singleLine = true,
        )
        Button(
            onClick = onDownloadRequested,
            enabled = state.imageUrl.trim().startsWith("https://") && !state.processing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.processing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.CloudDownload, contentDescription = null)
                Text(
                    text = stringResource(R.string.vault_detail_favicon_download),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
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
private fun FaviconEditorHeader(source: FaviconDraftSourceUiModel) {
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (source) {
                is FaviconDraftSourceUiModel.PrivateImage -> AsyncImage(
                    model = toLocalIconImageModel(source.localPath),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit,
                )
                is FaviconDraftSourceUiModel.BuiltIn -> VaultIcons.findDefinition(source.key)?.let { definition ->
                    Icon(
                        imageVector = definition.imageVector,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = iconColorForStorageToken(source.colorToken, MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
                FaviconDraftSourceUiModel.InferredDefault -> Unit
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.vault_detail_favicon_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FaviconIconLibrary(
    state: DetailFaviconEditorUiModel,
    onSearchChanged: (String) -> Unit,
    onSourceSelected: (FaviconDraftSourceUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.source as? FaviconDraftSourceUiModel.BuiltIn
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.search)) },
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            modifier = Modifier.weight(1f),
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
                    Box(contentAlignment = Alignment.Center) {
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
        val tokens = listOf<String?>(null) + VaultIconColorToken.entries.map { it.storageValue }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lazyRowItems(tokens) { token ->
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
