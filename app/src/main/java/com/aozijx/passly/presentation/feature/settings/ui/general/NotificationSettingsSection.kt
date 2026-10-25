package com.aozijx.passly.presentation.feature.settings.ui.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.presentation.feature.settings.main.general.NotificationSettingsAction
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.switchSettingsGroupItem
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

internal data class NotificationSettingsUiModel(
    val systemNotificationsEnabled: Boolean,
    val optionalMessagesEnabled: Boolean,
    val topics: List<NotificationTopicUiModel>,
)

internal data class NotificationTopicUiModel(
    val topic: MessageTopic,
    val enabled: Boolean,
)

@Composable
internal fun NotificationSettingsSection(
    state: NotificationSettingsUiModel,
    onAction: (NotificationSettingsAction) -> Unit,
) {
    val topicItems = state.topics.map { item ->
        val topic = item.topic
        switchSettingsGroupItem(
            key = "notifications.topic.${topic.name}",
            visible = state.optionalMessagesEnabled,
            title = stringResource(topic.titleRes),
            subtitle = stringResource(topic.summaryRes),
            checked = item.enabled,
            onCheckedChange = { enabled ->
                onAction(NotificationSettingsAction.SetTopicEnabled(topic, enabled))
            },
        )
    }

    SettingsSectionTitle(text = stringResource(R.string.settings_page_notifications))
    SegmentedSettingsGroup(
        items =
            listOf(
                switchSettingsGroupItem(
                    key = "notifications.system",
                    title = stringResource(R.string.settings_system_notifications),
                    subtitle = stringResource(R.string.settings_system_notifications_summary),
                    checked = state.systemNotificationsEnabled,
                    onCheckedChange = { enabled ->
                        onAction(
                            NotificationSettingsAction.SetSystemNotificationsEnabled(enabled),
                        )
                    },
                ),
                navigationSettingsGroupItem(
                    key = "notifications.system_settings",
                    title = stringResource(R.string.settings_system_notification_settings),
                    subtitle = stringResource(
                        R.string.settings_system_notification_settings_summary
                    ),
                    onClick = {
                        onAction(NotificationSettingsAction.OpenSystemNotificationSettings)
                    },
                ),
                switchSettingsGroupItem(
                    key = "notifications.optional_messages",
                    title = stringResource(R.string.settings_optional_notices),
                    subtitle = stringResource(R.string.settings_optional_notices_summary),
                    checked = state.optionalMessagesEnabled,
                    onCheckedChange = { enabled ->
                        onAction(NotificationSettingsAction.SetOptionalMessagesEnabled(enabled))
                    },
                ),
            ) + topicItems,
    )
}

private val MessageTopic.titleRes: Int
    get() = when (this) {
        MessageTopic.CLIPBOARD -> R.string.settings_topic_clipboard
        MessageTopic.APP_LIFECYCLE -> R.string.settings_topic_app_lifecycle
        MessageTopic.BACKUP -> R.string.settings_topic_backup
        MessageTopic.SECURITY -> R.string.settings_topic_security
        MessageTopic.DATABASE -> R.string.settings_topic_database
    }

private val MessageTopic.summaryRes: Int
    get() = when (this) {
        MessageTopic.CLIPBOARD -> R.string.settings_topic_clipboard_summary
        MessageTopic.APP_LIFECYCLE -> R.string.settings_topic_app_lifecycle_summary
        MessageTopic.BACKUP -> R.string.settings_topic_backup_summary
        MessageTopic.SECURITY -> R.string.settings_topic_security_summary
        MessageTopic.DATABASE -> R.string.settings_topic_database_summary
    }
