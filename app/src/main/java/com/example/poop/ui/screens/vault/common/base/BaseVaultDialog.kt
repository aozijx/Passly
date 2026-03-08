package com.example.poop.ui.screens.vault.common.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.poop.R

/**
 * 统一的弹窗脚手架（插槽架构的核心）
 *
 * 规范了所有新增/编辑弹窗的UI风格（圆角、按钮位置、间距等），
 * 后续添加新类型（例如信用卡、笔记）只需要关注表单内容（content）即可，
 * 彻底消除了每次写弹窗都要重复写 AlertDialog、TextButton 等样板代码的问题。
 */
@Composable
fun BaseVaultDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(R.string.action_save),
    dismissText: String = stringResource(R.string.action_cancel),
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                content()
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmEnabled
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(dismissText)
            }
        }
    )
}
