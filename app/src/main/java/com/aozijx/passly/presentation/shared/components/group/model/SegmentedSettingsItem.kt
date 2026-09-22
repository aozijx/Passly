package com.aozijx.passly.presentation.shared.components.group.model

import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable

/**
 * 分组项的不可变声明。
 *
 * [key] 必须在同一分组内唯一，并在插入、删除和重新排序时保持不变。
 */
class SegmentedSettingsItem(
    val key: String,
    val visible: Boolean = true,
    val content: @Composable (ListItemShapes) -> Unit,
)
