package com.example.poop.ui.screens.vault.components.dialog

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poop.R
import com.example.poop.ui.screens.profile.ImageType
import com.example.poop.ui.screens.vault.common.icons.VaultIcons
import com.example.poop.util.rememberImagePicker

@Composable
fun IconPickerDialog(
    onDismiss: () -> Unit,
    onIconSelected: (String?) -> Unit,
    onCustomImageSelected: (Uri) -> Unit,
    currentIconName: String?,
    currentCustomPath: String?
) {
    val pickPhoto = rememberImagePicker { uri, _ ->
        onCustomImageSelected(uri)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.icon_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.icon_picker_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(modifier = Modifier.height(300.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(56.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 选项 1：恢复默认（自动映射）
                        item {
                            IconItem(
                                name = null,
                                isSelected = currentIconName == null && currentCustomPath == null,
                                onClick = { onIconSelected(null) }
                            )
                        }

                        // 渲染所有预设图标
                        items(VaultIcons.allIcons.keys.toList()) { resId ->
                            IconItem(
                                name = resId,
                                isSelected = currentIconName == resId.toString() && currentCustomPath == null,
                                onClick = { onIconSelected(resId.toString()) }
                            )
                        }

                        // 选项 2：添加自定义图片 (末尾添加)
                        item {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (currentCustomPath != null) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { pickPhoto(ImageType.AVATAR) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = stringResource(R.string.icon_picker_action_add_photo),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun IconItem(
    name: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val iconColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = VaultIcons.getIconByRes(name),
                contentDescription = if (name != null) stringResource(name) else stringResource(R.string.label_default),
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
