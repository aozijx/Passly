package com.aozijx.passly.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class PlainExportDialogType {
    DatabaseError,   // 数据库迁移失败场景
    NormalExport     // 普通明文导出场景
}

// 定义对话框配置数据类（推荐方式）
data class PlainExportDialogConfig(
    val title: String,
    val message: String,
    val confirmText: String,
    val dismissText: String,
    val dismissColor: androidx.compose.ui.graphics.Color
)

@Composable
fun PlainExportDialog(
    modifier: Modifier = Modifier,
    type: PlainExportDialogType,
    onExportBackup: () -> Unit,
    onResetOrCancel: () -> Unit,
    onDismissRequest: () -> Unit = {}   // 默认不处理外部关闭
) {
    val config: PlainExportDialogConfig = when (type) {
        PlainExportDialogType.DatabaseError -> PlainExportDialogConfig(
            title = "数据库迁移失败",
            message = "检测到数据库版本冲突（可能由于移除图片列导致）。您可以尝试导出紧急备份以抢救数据，或直接清除数据并重置应用。\n\n注意：紧急导出为明文 JSON，请仅用于数据抢救，并妥善保管文件。",
            confirmText = "紧急导出备份（明文 JSON）",
            dismissText = "清除并重置",
            dismissColor = MaterialTheme.colorScheme.error
        )

        PlainExportDialogType.NormalExport -> PlainExportDialogConfig(
            title = "导出明文备份",
            message = "即将将所有条目以明文 JSON 格式导出。\n\n⚠️ 风险提示：\n• 文件中的所有密码均为未加密明文\n• 任何获得该文件的人都能直接读取您的全部密码\n• 导出后请立即转移至安全位置并从本机删除\n• 请勿上传至云盘或通过不安全渠道传输\n• 仅在需要数据迁移或自行加密备份时使用",
            confirmText = "我已了解风险，继续导出",
            dismissText = "取消",
            dismissColor = MaterialTheme.colorScheme.primary
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.padding(24.dp),
        title = { Text(config.title) },
        text = { Text(config.message) },
        confirmButton = {
            TextButton(onClick = onExportBackup) {
                Text(config.confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onResetOrCancel) {
                Text(
                    text = config.dismissText, color = config.dismissColor
                )
            }
        })
}