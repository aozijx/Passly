package com.aozijx.passly.features.detail.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.contract.DetailEvent
import com.aozijx.passly.features.detail.contract.DetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
    entry: VaultEntry,
    uiState: DetailUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onEvent: (DetailEvent) -> Unit,
    onBack: () -> Unit,
    onInteraction: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    LargeTopAppBar(
        title = {
            if (uiState.isEditingTitle) {
                OutlinedTextField(
                    value = uiState.editedTitle,
                    onValueChange = {
                        onEvent(DetailEvent.UpdateEditedTitle(it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    singleLine = true
                )
            } else {
                Text(
                    text = entry.title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.combinedClickable(
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(DetailEvent.StartTitleEdit)
                        },
                        onClick = { onInteraction() }
                    )
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                onInteraction()
                if (uiState.isEditingTitle) {
                    onEvent(DetailEvent.CancelTitleEdit)
                } else {
                    onBack()
                }
            }) {
                Icon(
                    imageVector = if (uiState.isEditingTitle) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (uiState.isEditingTitle) "取消" else "返回"
                )
            }
        },
        actions = {
            if (uiState.isEditingTitle) {
                TextButton(onClick = {
                    onInteraction()
                    onEvent(DetailEvent.SaveTitle)
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, "保存")
                        Spacer(Modifier.width(4.dp))
                        Text("保存")
                    }
                }
            } else {
                IconButton(onClick = {
                    onInteraction()
                    onEvent(DetailEvent.ToggleFavorite)
                }) {
                    Icon(
                        imageVector = if (entry.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (entry.favorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors()
    )
}