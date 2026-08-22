package com.aozijx.passly.feature.detail.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.PasslyOutlinedTextField
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.feature.detail.contract.DetailUiAction
import com.aozijx.passly.feature.detail.contract.DetailUiState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailTopBar(
    entry: Entry,
    uiState: DetailUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onAction: (DetailUiAction) -> Unit,
    onBack: () -> Unit,
    onInteraction: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val titleInteractionSource = remember { MutableInteractionSource() }

    LargeFlexibleTopAppBar(
        title = {
            if (uiState.isEditingTitle) {
                PasslyOutlinedTextField(
                    value = uiState.editedTitle,
                    onValueChange = {
                        onAction(DetailUiAction.UpdateEditedTitle(it))
                    },
                    label = "",
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            onInteraction()
                            onAction(DetailUiAction.SaveTitle)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.save),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            } else {
                Text(
                    text = entry.title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.combinedClickable(
                        interactionSource = titleInteractionSource,
                        indication = null,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAction(DetailUiAction.StartTitleEdit)
                        },
                        onClick = { onInteraction() }
                    )
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = {
                onInteraction()
                onAction(DetailUiAction.ToggleFavorite)
            }) {
                Icon(
                    imageVector = if (entry.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = if (entry.favorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors()
    )
}
