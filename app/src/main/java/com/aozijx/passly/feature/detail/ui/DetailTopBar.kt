package com.aozijx.passly.feature.detail.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.ui.text.input.TextFieldValue
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.PasslyOutlinedTextField
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.DetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
    entry: VaultEntry,
    uiState: DetailUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onEvent: (DetailIntent) -> Unit,
    onInteraction: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val titleInteractionSource = remember { MutableInteractionSource() }

    LargeTopAppBar(
        title = {
            if (uiState.isEditingTitle) {
                PasslyOutlinedTextField(
                    value = TextFieldValue(uiState.editedTitle),
                    onValueChange = {
                        onEvent(DetailIntent.UpdateEditedTitle(it.text))
                    },
                    label = "",
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            onInteraction()
                            onEvent(DetailIntent.SaveTitle)
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
                            onEvent(DetailIntent.StartTitleEdit)
                        },
                        onClick = { onInteraction() }
                    )
                )
            }
        },
        navigationIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回"
            )
        },
        actions = {
            IconButton(onClick = {
                onInteraction()
                onEvent(DetailIntent.ToggleFavorite)
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
