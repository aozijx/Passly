package com.aozijx.passly.features.vault.components.topbar.compoents

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@Composable
fun CustomExportMenuItem(
    text: String, leadingIcon: @Composable () -> Unit, onClick: () -> Unit, onLongClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current   // 获取震动对象
    Surface(

        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {
                // 这里可以加上关闭菜单的逻辑
                onClick()
            }, onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            }), color = Color.Transparent, contentColor = LocalContentColor.current
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon()
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text)
        }
    }
}
