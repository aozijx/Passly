package com.aozijx.passly.features.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.model.VaultEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MetadataSection(entry: VaultEntry) {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MetadataText("创建于: ${df.format(Date(entry.createdAt ?: 0))}")
        entry.updatedAt?.let {
            MetadataText("最后修改: ${df.format(Date(it))}")
        }
        MetadataText("使用次数: ${entry.usageCount} 次")
    }
}

@Composable
fun MetadataText(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline
    )
}
