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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun HistoryTimelineSection(historyList: List<VaultHistory>) {
    if (historyList.isEmpty()) return

    InfoGroupCard(title = stringResource(R.string.vault_detail_history_label)) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            historyList.take(10).forEach { history ->
                HistoryItem(history)
            }
        }
    }
}

@Composable
private fun HistoryItem(history: VaultHistory) {
    val dateFormat = rememberDateFormat()
    val timeFormat = rememberTimeFormat()

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
        VaultHistory.HistoryType.UPDATE -> {
            if (history.oldValue == null) "创建了条目"
            else "更新了 ${history.fieldName}"
        }

        VaultHistory.HistoryType.ACCESS -> "查看了 ${history.fieldName}"
        VaultHistory.HistoryType.COPY -> "复制了 ${history.fieldName}"
        VaultHistory.HistoryType.AUTOFILL -> "使用了自动填充"
    }
}

@Composable
private fun getHistoryColor(type: VaultHistory.HistoryType): Color {
    return when (type) {
        VaultHistory.HistoryType.UPDATE -> MaterialTheme.colorScheme.primary
        VaultHistory.HistoryType.ACCESS -> MaterialTheme.colorScheme.secondary
        VaultHistory.HistoryType.COPY -> MaterialTheme.colorScheme.tertiary
        VaultHistory.HistoryType.AUTOFILL -> Color(0xFF4CAF50)
    }
}

@Composable
fun rememberDateFormat() = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

@Composable
fun rememberTimeFormat() = SimpleDateFormat("HH:mm", Locale.getDefault())