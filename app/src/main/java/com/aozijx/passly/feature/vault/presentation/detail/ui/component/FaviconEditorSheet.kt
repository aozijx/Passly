package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButtonSize
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.core.ui.components.passlySheetTitleStyle
import com.aozijx.passly.feature.vault.presentation.detail.DetailFaviconEditorState
import com.aozijx.passly.feature.vault.presentation.detail.DetailFaviconSource
import com.aozijx.passly.feature.vault.presentation.detail.DetailFaviconSource.ColorSelection
import com.aozijx.passly.feature.vault.presentation.detail.DetailFaviconTab
import com.aozijx.passly.feature.vault.presentation.detail.DetailFaviconWorkState
import com.aozijx.passly.feature.vault.presentation.detail.toStorageToken
import com.aozijx.passly.presentation.shared.components.VaultIconColorToken
import com.aozijx.passly.presentation.shared.components.VaultIcons
import com.aozijx.passly.presentation.shared.components.iconColorForStorageToken
import com.aozijx.passly.presentation.shared.media.toLocalIconImageModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FaviconEditorSheet(
    state: DetailFaviconEditorState.Open,
    isSaving: Boolean,
    onTabSelected: (DetailFaviconTab) -> Unit,
    onSearchChanged: (String) -> Unit,
    onDefaultSelected: () -> Unit,
    onIconSelected: (String) -> Unit,
    onColorSelected: (ColorSelection) -> Unit,
    onUploadRequested: () -> Unit,
    onImageUrlChanged: (String) -> Unit,
    onDownloadRequested: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    val processing = state.work == DetailFaviconWorkState.Processing
    val dismissAllowed by rememberUpdatedState(!isSaving && !processing)
    val sheetState =
        rememberBottomSheetState(
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
            modifier =
                Modifier
                    .fillMaxWidth()
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
                val iconLibraryLabel = stringResource(R.string.vault_detail_favicon_library)
                val customImageLabel = stringResource(R.string.vault_detail_favicon_custom_image)
                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    overflowIndicator = { menuState ->
                        ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                    },
                ) {
                    DetailFaviconTab.entries.forEach { tab ->
                        val label =
                            when (tab) {
                                DetailFaviconTab.ICON_LIBRARY -> iconLibraryLabel
                                DetailFaviconTab.CUSTOM_IMAGE -> customImageLabel
                            }
                        val icon =
                            when (tab) {
                                DetailFaviconTab.ICON_LIBRARY -> Icons.Default.Apps
                                DetailFaviconTab.CUSTOM_IMAGE -> Icons.Default.Image
                            }
                        toggleableItem(
                            checked = state.selectedTab == tab,
                            label = label,
                            onCheckedChange = { checked ->
                                if (checked) onTabSelected(tab)
                            },
                            icon = { Icon(imageVector = icon, contentDescription = null) },
                            weight = 1f,
                            enabled = dismissAllowed,
                        )
                    }
                }
                when (state.selectedTab) {
                    DetailFaviconTab.ICON_LIBRARY -> {
                        FaviconIconLibrary(
                            state = state,
                            onSearchChanged = onSearchChanged,
                            onColorSelected = onColorSelected,
                            onIconSelected = onIconSelected,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    DetailFaviconTab.CUSTOM_IMAGE -> {
                        FaviconCustomImage(
                            state = state,
                            onUploadRequested = onUploadRequested,
                            onImageUrlChanged = onImageUrlChanged,
                            onDownloadRequested = onDownloadRequested,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            when (val work = state.work) {
                is DetailFaviconWorkState.Failed -> {
                    Text(
                        text = stringResource(work.messageRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                DetailFaviconWorkState.Idle,
                DetailFaviconWorkState.Processing,
                -> {
                    Unit
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDefaultSelected,
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
                        LoadingIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
    if (state.showDiscardConfirmation) {
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun FaviconCustomImage(
    state: DetailFaviconEditorState.Open,
    onUploadRequested: () -> Unit,
    onImageUrlChanged: (String) -> Unit,
    onDownloadRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val processing = state.work == DetailFaviconWorkState.Processing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilledTonalButton(
            onClick = onUploadRequested,
            enabled = !processing,
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
            enabled = !processing,
            modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
            label = { Text(stringResource(R.string.vault_detail_favicon_image_url)) },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            singleLine = true,
        )
        Button(
            onClick = onDownloadRequested,
            enabled = state.canSubmitImageUrl && !processing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (processing) {
                LoadingIndicator(modifier = Modifier.size(24.dp))
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

@Composable
private fun FaviconEditorHeader(source: DetailFaviconSource) {
    val text =
        when (source) {
            DetailFaviconSource.InferredDefault -> {
                stringResource(R.string.vault_detail_favicon_default)
            }

            is DetailFaviconSource.BuiltIn -> {
                VaultIcons
                    .findDefinition(source.key)
                    ?.let { stringResource(it.labelRes) }
                    ?: stringResource(R.string.vault_detail_favicon_built_in)
            }

            is DetailFaviconSource.PrivateImage -> {
                stringResource(R.string.vault_detail_favicon_custom_image)
            }
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
                is DetailFaviconSource.PrivateImage -> {
                    AsyncImage(
                        model = toLocalIconImageModel(source.localPath)?.uri,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Fit,
                    )
                }

                is DetailFaviconSource.BuiltIn -> {
                    VaultIcons.findDefinition(source.key)?.let { definition ->
                        Icon(
                            imageVector = definition.imageVector,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = iconColorForStorageToken(source.color.toStorageToken(), MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                }

                DetailFaviconSource.InferredDefault -> {
                    Unit
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.vault_detail_favicon_title),
                    style = passlySheetTitleStyle(),
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
    state: DetailFaviconEditorState.Open,
    onSearchChanged: (String) -> Unit,
    onColorSelected: (ColorSelection) -> Unit,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.source as? DetailFaviconSource.BuiltIn
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
            singleLine = true,
            label = { Text(stringResource(R.string.search)) },
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.matchingIcons, key = { it.key }) { definition ->
                val isSelected = selected?.let { VaultIcons.findDefinition(it.key)?.key } == definition.key
                FilledTonalToggleButton(
                    checked = isSelected,
                    onCheckedChange = { checked ->
                        if (checked) onIconSelected(definition.key)
                    },
                    modifier = Modifier.size(52.dp),
                    buttonSize = ToggleButtonSize.ExtraSmall,
                ) {
                    Icon(
                        imageVector = definition.imageVector,
                        contentDescription = stringResource(definition.labelRes),
                        tint =
                            iconColorForStorageToken(
                                selected?.color?.toStorageToken(),
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                    )
                }
            }
        }
        val colors =
            listOf(ColorSelection.Automatic to R.string.vault_detail_favicon_color_automatic) +
                VaultIconColorToken.entries.map { token ->
                    ColorSelection.Token(token.storageValue) to token.faviconColorLabelRes
                }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lazyRowItems(
                colors,
                key = { (selection, _) ->
                    selection.toStorageToken()?.let { "color:$it" } ?: "color:automatic"
                },
            ) { (colorSelection, labelRes) ->
                val color =
                    iconColorForStorageToken(
                        colorSelection.toStorageToken(),
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                val colorLabel = stringResource(labelRes)
                FilledTonalToggleButton(
                    checked = selected?.color == colorSelection,
                    onCheckedChange = { checked ->
                        if (checked) onColorSelected(colorSelection)
                    },
                    enabled = selected != null,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .semantics { contentDescription = colorLabel },
                    buttonSize = ToggleButtonSize.ExtraSmall,
                ) {
                    Surface(modifier = Modifier.size(20.dp), shape = CircleShape, color = color) {
                        if (colorSelection == ColorSelection.Automatic) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.surface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val VaultIconColorToken.faviconColorLabelRes: Int
    get() =
        when (this) {
            VaultIconColorToken.PRIMARY -> R.string.vault_detail_favicon_color_primary
            VaultIconColorToken.SECONDARY -> R.string.vault_detail_favicon_color_secondary
            VaultIconColorToken.TERTIARY -> R.string.vault_detail_favicon_color_tertiary
            VaultIconColorToken.ERROR -> R.string.vault_detail_favicon_color_error
            VaultIconColorToken.NEUTRAL -> R.string.vault_detail_favicon_color_neutral
        }
