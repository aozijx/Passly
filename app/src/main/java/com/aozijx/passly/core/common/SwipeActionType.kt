package com.aozijx.passly.core.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class SwipeActionType(
    val displayName: String,
    val icon: ImageVector?,
    val requiresConfirm: Boolean = false,
    val copyField: String? = null
) {
    DELETE(
        displayName = "删除", icon = Icons.Default.Delete, requiresConfirm = true
    ),
    EDIT(
        displayName = "编辑", icon = Icons.Default.Edit, requiresConfirm = true
    ),
    DETAIL(
        displayName = "详情", icon = Icons.Default.Info, requiresConfirm = false
    ),
    COPY_PASSWORD(
        displayName = "复制密码", icon = Icons.Default.ContentCopy, copyField = "password"
    ),
    COPY_USERNAME(
        displayName = "复制账号", icon = Icons.Default.Person, copyField = "username"
    ),
    DISABLED(
        displayName = "禁用", icon = null, requiresConfirm = false
    );

    companion object {
        fun fromString(value: String): SwipeActionType {
            return entries.find { it.name == value } ?: DISABLED
        }
    }
}
