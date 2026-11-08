package com.aozijx.passly.feature.vault.presentation.detail.ui.component

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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import com.aozijx.passly.feature.vault.presentation.detail.DetailActivityFilter
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailActivityHistoryUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailActivityUiModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActivityTimelineSection(
    selectedFilter: DetailActivityFilter,
    onFilterChanged: (DetailActivityFilter) -> Unit,
    state: DetailActivityHistoryUiState,
    onRetry: () -> Unit,
) {
    InfoGroupCard(title = stringResource(R.string.vault_detail_history_label)) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActivityFilterChip(
                    selected = selectedFilter == DetailActivityFilter.ALL,
                    onClick = { onFilterChanged(DetailActivityFilter.ALL) },
                    label = stringResource(R.string.all),
                )
                ActivityFilterChip(
                    selected = selectedFilter == DetailActivityFilter.ACTIONS,
                    onClick = { onFilterChanged(DetailActivityFilter.ACTIONS) },
                    label = stringResource(R.string.vault_detail_history_updates),
                )
                ActivityFilterChip(
                    selected = selectedFilter == DetailActivityFilter.AUTOFILL,
                    onClick = { onFilterChanged(DetailActivityFilter.AUTOFILL) },
                    label = stringResource(R.string.vault_detail_activity_autofill),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                DetailActivityHistoryUiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.padding(vertical = 8.dp).size(32.dp))
                }

                DetailActivityHistoryUiState.FailedEmpty -> {
                    Text(
                        text = stringResource(R.string.vault_detail_history_load_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.vault_detail_history_retry))
                    }
                }

                is DetailActivityHistoryUiState.FailedItems -> {
                    Column {
                        Text(
                            text = stringResource(R.string.vault_detail_history_load_failed),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.vault_detail_history_retry))
                        }
                        ActivityItems(state.activities)
                    }
                }

                DetailActivityHistoryUiState.ReadyEmpty ->
                    Text(
                        text = stringResource(R.string.vault_detail_history_empty_filtered),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )

                is DetailActivityHistoryUiState.ReadyItems -> ActivityItems(state.activities)
            }
        }
    }
}

@Composable
private fun ActivityItems(activities: List<DetailActivityUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        activities.take(20).forEach { activity ->
            ActivityItem(activity)
        }
    }
}

@Composable
private fun ActivityFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    )
}

@Composable
private fun ActivityItem(activity: DetailActivityUiModel) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale) }

    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(getActivityColor(activity.type)),
            )
            Box(
                modifier =
                    Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = formatActivityDescription(activity),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = dateFormat.format(Date(activity.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun formatActivityDescription(activity: DetailActivityUiModel): String {
    val description =
        when (activity.type) {
            ActivityType.CREATE -> stringResource(R.string.vault_detail_activity_create)
            ActivityType.UPDATE -> stringResource(R.string.vault_detail_activity_update)
            ActivityType.VERSION_RESTORED -> stringResource(R.string.vault_detail_activity_version_restored)
            ActivityType.DELETE -> stringResource(R.string.vault_detail_activity_delete)
            ActivityType.RESTORE -> stringResource(R.string.vault_detail_activity_restore)
            ActivityType.AUTOFILL -> stringResource(R.string.vault_detail_activity_autofill)
            ActivityType.COPY_PASSWORD -> stringResource(R.string.vault_detail_activity_copy_password)
            ActivityType.COPY_USERNAME -> stringResource(R.string.vault_detail_activity_copy_username)
            ActivityType.VIEW -> stringResource(R.string.vault_detail_activity_view)
            ActivityType.EXPORT -> stringResource(R.string.vault_detail_activity_export)
            ActivityType.IMPORT -> stringResource(R.string.vault_detail_activity_import)
        }
    return when (val context = activity.context) {
        is EntryActivityContext.ContentRevision -> {
            stringResource(
                R.string.vault_detail_activity_revision_format,
                description,
                context.version.value,
            )
        }

        EntryActivityContext.Usage, EntryActivityContext.Lifecycle -> {
            description
        }
    }
}

@Composable
private fun getActivityColor(type: ActivityType): Color =
    when (type) {
        ActivityType.CREATE -> Color(0xFF4CAF50)
        ActivityType.UPDATE -> MaterialTheme.colorScheme.primary
        ActivityType.VERSION_RESTORED -> MaterialTheme.colorScheme.tertiary
        ActivityType.DELETE -> MaterialTheme.colorScheme.error
        ActivityType.RESTORE -> MaterialTheme.colorScheme.secondary
        ActivityType.AUTOFILL -> MaterialTheme.colorScheme.primary
        ActivityType.COPY_PASSWORD, ActivityType.COPY_USERNAME -> MaterialTheme.colorScheme.secondary
        ActivityType.VIEW -> MaterialTheme.colorScheme.outline
        ActivityType.EXPORT -> MaterialTheme.colorScheme.tertiary
        ActivityType.IMPORT -> Color(0xFF8BC34A)
    }
