package com.aozijx.passly.features.detail.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.core.VaultHistory
import com.aozijx.passly.features.detail.components.InfoGroupCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter {
    ALL, UPDATES, ACTIONS
}

@Composable
fun HistoryTimelineSection(historyList: List<VaultHistory>) {
    if (historyList.isEmpty()) return

    var currentFilter by remember { mutableStateOf(HistoryFilter.ALL) }

    val filteredList = remember(historyList, currentFilter) {
        when (currentFilter) {
            HistoryFilter.ALL -> historyList
            HistoryFilter.UPDATES -> historyList.filter {
                it.changeType == VaultHistory.HistoryType.UPDATE ||
                        it.changeType == VaultHistory.HistoryType.CREATE
            }

            HistoryFilter.ACTIONS -> historyList.filter {
                it.changeType != VaultHistory.HistoryType.UPDATE &&
                        it.changeType != VaultHistory.HistoryType.CREATE
            }
        }
    }

    InfoGroupCard(title = stringResource(R.string.vault_detail_history_label)) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // 过滤选项组 (Chip 组) - 替代原本的开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilterChip(
                    selected = currentFilter == HistoryFilter.ALL,
                    onClick = { currentFilter = HistoryFilter.ALL },
                    label = stringResource(R.string.vault_detail_history_all)
                )
                HistoryFilterChip(
                    selected = currentFilter == HistoryFilter.UPDATES,
                    onClick = { currentFilter = HistoryFilter.UPDATES },
                    label = stringResource(R.string.vault_detail_history_updates)
                )
                HistoryFilterChip(
                    selected = currentFilter == HistoryFilter.ACTIONS,
                    onClick = { currentFilter = HistoryFilter.ACTIONS },
                    label = stringResource(R.string.vault_detail_history_actions)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredList.isEmpty()) {
                Text(
                    text = stringResource(R.string.vault_detail_history_empty_filtered),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    filteredList.take(20).forEach { history ->
                        HistoryItem(history)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
    )
}

@Composable
private fun HistoryItem(history: VaultHistory) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(getHistoryColor(history.changeType))
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = formatHistoryDescription(history),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${dateFormat.format(Date(history.changedAt))} ${
                    timeFormat.format(
                        Date(
                            history.changedAt
                        )
                    )
                }",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun formatHistoryDescription(history: VaultHistory): String {
    return when (history.changeType) {
        VaultHistory.HistoryType.CREATE -> "创建了该条目"
        VaultHistory.HistoryType.UPDATE -> "修改了 ${history.fieldName}"
        VaultHistory.HistoryType.ACCESS -> "查看了详情"
        VaultHistory.HistoryType.COPY -> "复制了 ${history.fieldName}"
        VaultHistory.HistoryType.AUTOFILL -> "执行了自动填充"
    }
}

@Composable
private fun getHistoryColor(type: VaultHistory.HistoryType): Color {
    return when (type) {
        VaultHistory.HistoryType.CREATE -> Color(0xFF2196F3) // 蓝色
        VaultHistory.HistoryType.UPDATE -> MaterialTheme.colorScheme.primary
        VaultHistory.HistoryType.ACCESS -> Color(0xFF9C27B0) // 紫色
        VaultHistory.HistoryType.COPY -> Color(0xFF00BCD4)   // 青色
        VaultHistory.HistoryType.AUTOFILL -> Color(0xFF4CAF50) // 绿色
    }
}