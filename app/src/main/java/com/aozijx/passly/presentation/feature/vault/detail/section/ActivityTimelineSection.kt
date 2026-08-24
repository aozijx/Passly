package com.aozijx.passly.presentation.feature.vault.detail.section

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
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.presentation.ui.vault.detail.component.InfoGroupCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ActivityFilter {
    ALL,
    ACTIONS,
    AUTOFILL
}

@Composable
fun ActivityTimelineSection(activityList: List<EntryActivity>) {
    var currentFilter by remember { mutableStateOf(ActivityFilter.ALL) }

    val filteredList = remember(activityList, currentFilter) {
        when (currentFilter) {
            ActivityFilter.ALL -> activityList
            ActivityFilter.ACTIONS -> activityList.filter {
                it.activityType in listOf(
                    ActivityType.CREATE,
                    ActivityType.UPDATE,
                    ActivityType.SENSITIVE_CHANGE,
                    ActivityType.DELETE,
                    ActivityType.RESTORE,
                )
            }
            ActivityFilter.AUTOFILL -> activityList.filter {
                it.activityType == ActivityType.AUTOFILL
            }
        }
    }

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
                ActivityFilterChip(
                    selected = currentFilter == ActivityFilter.ALL,
                    onClick = { currentFilter = ActivityFilter.ALL },
                    label = stringResource(R.string.tab_all)
                )
                ActivityFilterChip(
                    selected = currentFilter == ActivityFilter.ACTIONS,
                    onClick = { currentFilter = ActivityFilter.ACTIONS },
                    label = stringResource(R.string.vault_detail_history_updates)
                )
                ActivityFilterChip(
                    selected = currentFilter == ActivityFilter.AUTOFILL,
                    onClick = { currentFilter = ActivityFilter.AUTOFILL },
                    label = stringResource(R.string.vault_detail_activity_autofill)
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
                    filteredList.take(20).forEach { activity ->
                        ActivityItem(activity)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityFilterChip(
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
private fun ActivityItem(activity: EntryActivity) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(getActivityColor(activity.activityType))
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
                text = formatActivityDescription(activity),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${dateFormat.format(Date(activity.createdAt))} ${
                    timeFormat.format(
                        Date(
                            activity.createdAt
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
private fun formatActivityDescription(activity: EntryActivity): String {
    return when (activity.activityType) {
        ActivityType.CREATE -> stringResource(R.string.vault_detail_activity_create)
        ActivityType.UPDATE -> stringResource(R.string.vault_detail_activity_update)
        ActivityType.SENSITIVE_CHANGE -> stringResource(R.string.vault_detail_activity_sensitive_change)
        ActivityType.DELETE -> stringResource(R.string.vault_detail_activity_delete)
        ActivityType.RESTORE -> stringResource(R.string.vault_detail_activity_restore)
        ActivityType.AUTOFILL -> stringResource(R.string.vault_detail_activity_autofill)
        ActivityType.COPY_PASSWORD -> stringResource(R.string.vault_detail_activity_copy_password)
        ActivityType.COPY_USERNAME -> stringResource(R.string.vault_detail_activity_copy_username)
        ActivityType.VIEW -> stringResource(R.string.vault_detail_activity_view)
        ActivityType.EXPORT -> stringResource(R.string.vault_detail_activity_export)
        ActivityType.IMPORT -> stringResource(R.string.vault_detail_activity_import)
    }
}

@Composable
private fun getActivityColor(type: ActivityType): Color {
    return when (type) {
        ActivityType.CREATE -> Color(0xFF4CAF50)
        ActivityType.UPDATE -> MaterialTheme.colorScheme.primary
        ActivityType.SENSITIVE_CHANGE -> MaterialTheme.colorScheme.tertiary
        ActivityType.DELETE -> MaterialTheme.colorScheme.error
        ActivityType.RESTORE -> MaterialTheme.colorScheme.secondary
        ActivityType.AUTOFILL -> MaterialTheme.colorScheme.primary
        ActivityType.COPY_PASSWORD, ActivityType.COPY_USERNAME -> MaterialTheme.colorScheme.secondary
        ActivityType.VIEW -> MaterialTheme.colorScheme.outline
        ActivityType.EXPORT -> MaterialTheme.colorScheme.tertiary
        ActivityType.IMPORT -> Color(0xFF8BC34A)
    }
}
