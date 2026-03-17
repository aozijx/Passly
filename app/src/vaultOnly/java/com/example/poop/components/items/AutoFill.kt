package com.example.poop.components.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppSettingsAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poop.MainViewModel
import com.example.poop.R
import com.example.poop.common.icons.VaultItemIcon
import com.example.poop.data.VaultEntry

@Composable
fun AutoFillItem(
    entry: VaultEntry,
    viewModel: MainViewModel
) {
    // 判断是否为自动抓取的“静默”数据
    val isAutoCaptured = entry.category == stringResource(R.string.category_autofill)

    Card(
        onClick = { viewModel.showDetail(entry) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标
            VaultItemIcon(item = entry)

            Spacer(modifier = Modifier.width(16.dp))

            // 中间信息区
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // 如果是自动抓取，显示安全警告小图标
                    if (isAutoCaptured) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Security,
                            contentDescription = stringResource(R.string.autofill_pending_verification),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Autofill 关联信息组件
                val associatedInfo = entry.associatedDomain ?: entry.associatedAppPackage
                if (!associatedInfo.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (entry.associatedDomain != null) Icons.Default.Language else Icons.Default.AppSettingsAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = associatedInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 右侧分类标签
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = entry.category,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
