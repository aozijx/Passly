package com.aozijx.passly.presentation.ui.vault.detail.component

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
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailActivityTypeUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailActivityUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ActivityFilter {
    ALL,
    ACTIONS,
    AUTOFILL
}

@Composable
fun ActivityTimelineSection(activityList: List<DetailActivityUiModel>) {
    var currentFilter by remember { mutableStateOf(ActivityFilter.ALL) }

    val filteredList = remember(activityList, currentFilter) {
        when (currentFilter) {
            ActivityFilter.ALL -> activityList
            ActivityFilter.ACTIONS -> activityList.filter {
                it.type in listOf(
                    DetailActivityTypeUiModel.CREATE,
                    DetailActivityTypeUiModel.UPDATE,
                    DetailActivityTypeUiModel.SENSITIVE_CHANGE,
                    DetailActivityTypeUiModel.DELETE,
                    DetailActivityTypeUiModel.RESTORE,
                )
            }
            ActivityFilter.AUTOFILL -> activityList.filter {
                it.type == DetailActivityTypeUiModel.AUTOFILL
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
private fun ActivityItem(activity: DetailActivityUiModel) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(getActivityColor(activity.type))
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
private fun formatActivityDescription(activity: DetailActivityUiModel): String {
    return when (activity.type) {
        DetailActivityTypeUiModel.CREATE -> stringResource(R.string.vault_detail_activity_create)
        DetailActivityTypeUiModel.UPDATE -> stringResource(R.string.vault_detail_activity_update)
        DetailActivityTypeUiModel.SENSITIVE_CHANGE -> stringResource(R.string.vault_detail_activity_sensitive_change)
        DetailActivityTypeUiModel.DELETE -> stringResource(R.string.vault_detail_activity_delete)
        DetailActivityTypeUiModel.RESTORE -> stringResource(R.string.vault_detail_activity_restore)
        DetailActivityTypeUiModel.AUTOFILL -> stringResource(R.string.vault_detail_activity_autofill)
        DetailActivityTypeUiModel.COPY_PASSWORD -> stringResource(R.string.vault_detail_activity_copy_password)
        DetailActivityTypeUiModel.COPY_USERNAME -> stringResource(R.string.vault_detail_activity_copy_username)
        DetailActivityTypeUiModel.VIEW -> stringResource(R.string.vault_detail_activity_view)
        DetailActivityTypeUiModel.EXPORT -> stringResource(R.string.vault_detail_activity_export)
        DetailActivityTypeUiModel.IMPORT -> stringResource(R.string.vault_detail_activity_import)
    }
}

@Composable
private fun getActivityColor(type: DetailActivityTypeUiModel): Color {
    return when (type) {
        DetailActivityTypeUiModel.CREATE -> Color(0xFF4CAF50)
        DetailActivityTypeUiModel.UPDATE -> MaterialTheme.colorScheme.primary
        DetailActivityTypeUiModel.SENSITIVE_CHANGE -> MaterialTheme.colorScheme.tertiary
        DetailActivityTypeUiModel.DELETE -> MaterialTheme.colorScheme.error
        DetailActivityTypeUiModel.RESTORE -> MaterialTheme.colorScheme.secondary
        DetailActivityTypeUiModel.AUTOFILL -> MaterialTheme.colorScheme.primary
        DetailActivityTypeUiModel.COPY_PASSWORD, DetailActivityTypeUiModel.COPY_USERNAME -> MaterialTheme.colorScheme.secondary
        DetailActivityTypeUiModel.VIEW -> MaterialTheme.colorScheme.outline
        DetailActivityTypeUiModel.EXPORT -> MaterialTheme.colorScheme.tertiary
        DetailActivityTypeUiModel.IMPORT -> Color(0xFF8BC34A)
    }
}
