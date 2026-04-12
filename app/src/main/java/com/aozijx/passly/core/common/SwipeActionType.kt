package com.aozijx.passly.core.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.domain.model.FieldKey

enum class SwipeActionType(
    val displayName: String,
    val icon: ImageVector?,
    val requiresConfirm: Boolean = false,
    val copyField: FieldKey? = null
) {
    DELETE(
        displayName = "删除", icon = Icons.Default.Delete, requiresConfirm = true
    ),
    DETAIL(
        displayName = "详情", icon = Icons.Default.Info, requiresConfirm = false
    ),
    COPY_PASSWORD(
        displayName = "复制密码", icon = Icons.Default.ContentCopy, copyField = FieldKey.PASSWORD
    ),
    COPY_USERNAME(
        displayName = "复制账号", icon = Icons.Default.Person, copyField = FieldKey.USERNAME
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
