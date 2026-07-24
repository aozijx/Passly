package com.aozijx.passly.feature.detail.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.revision.EntryRevision
import com.aozijx.passly.feature.detail.components.InfoGroupCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ActivityTab { VERSION, ACTIVITY }

@Composable
fun ActivitySection(
    historyList: List<EntryRevision>,
    activityList: List<EntryActivity>,
    onRestore: (historyId: String) -> Unit
) {
    var currentTab by remember { mutableStateOf(ActivityTab.ACTIVITY) }

    InfoGroupCard(title = stringResource(R.string.vault_detail_history_label)) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentTab == ActivityTab.VERSION,
                    onClick = { currentTab = ActivityTab.VERSION },
                    label = { Text(stringResource(R.string.vault_detail_history_tab_version)) }
                )
                FilterChip(
                    selected = currentTab == ActivityTab.ACTIVITY,
                    onClick = { currentTab = ActivityTab.ACTIVITY },
                    label = { Text(stringResource(R.string.vault_detail_history_tab_activity)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (currentTab) {
                ActivityTab.VERSION -> VersionTab(historyList, onRestore)
                ActivityTab.ACTIVITY -> ActivityTabContent(activityList)
            }
        }
    }
}

@Composable
private fun VersionTab(historyList: List<EntryRevision>, onRestore: (String) -> Unit) {
    if (historyList.isEmpty()) return

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        historyList.take(20).forEach { history ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "v${history.entry.entryVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${dateFormat.format(Date(history.entry.createdAt))} ${
                            timeFormat.format(Date(history.entry.createdAt))
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onRestore(history.revisionId) }) {
                    Text(stringResource(R.string.vault_detail_history_restore))
                }
            }
        }
    }
}

@Composable
private fun ActivityTabContent(activityList: List<EntryActivity>) {
    if (activityList.isEmpty()) return

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        activityList.take(50).forEach { activity ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activityDescription(activity),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row {
                        Text(
                            text = "${dateFormat.format(Date(activity.createdAt))} ${
                                timeFormat.format(Date(activity.createdAt))
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!activity.source.isNullOrBlank()) {
                            Text(
                                text = "  ${activity.source}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun activityDescription(activity: EntryActivity): String = when (activity.activityType) {
    ActivityType.CREATE -> stringResource(R.string.vault_detail_activity_create)
    ActivityType.UPDATE -> stringResource(R.string.vault_detail_activity_update)
    ActivityType.DELETE -> stringResource(R.string.vault_detail_activity_delete)
    ActivityType.RESTORE -> stringResource(R.string.vault_detail_activity_restore)
    ActivityType.AUTOFILL -> stringResource(R.string.vault_detail_activity_autofill)
    ActivityType.COPY_PASSWORD -> stringResource(R.string.vault_detail_activity_copy_password)
    else -> ""
}
