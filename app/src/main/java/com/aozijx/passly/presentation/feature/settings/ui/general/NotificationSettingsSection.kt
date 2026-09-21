package com.aozijx.passly.presentation.feature.settings.ui.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.ui.shared.components.group.switchSettingsGroupItem
import com.aozijx.passly.presentation.ui.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

internal data class NotificationSettingsUiModel(
    val systemNotificationsEnabled: Boolean,
    val optionalMessagesEnabled: Boolean,
    val topics: List<NotificationTopicUiModel>,
)

internal data class NotificationTopicUiModel(
    val topic: NotificationTopic,
    val enabled: Boolean,
)

internal enum class NotificationTopic {
    CLIPBOARD, APP_LIFECYCLE, BACKUP, SECURITY, DATABASE,
}

internal interface NotificationSettingsEventHandler {
    fun onSystemNotificationsEnabledChanged(enabled: Boolean)
    fun onOpenSystemNotificationSettings()
    fun onOptionalMessagesEnabledChanged(enabled: Boolean)
    fun onTopicEnabledChanged(topic: NotificationTopic, enabled: Boolean)
}

@Composable
internal fun NotificationSettingsSection(
    state: NotificationSettingsUiModel,
    eventHandler: NotificationSettingsEventHandler,
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
                eventHandler.onTopicEnabledChanged(topic, enabled)
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
                    onCheckedChange = eventHandler::onSystemNotificationsEnabledChanged,
                ),
                navigationSettingsGroupItem(
                    key = "notifications.system_settings",
                    title = stringResource(R.string.settings_system_notification_settings),
                    subtitle = stringResource(
                        R.string.settings_system_notification_settings_summary
                    ),
                    onClick = eventHandler::onOpenSystemNotificationSettings,
                ),
                switchSettingsGroupItem(
                    key = "notifications.optional_messages",
                    title = stringResource(R.string.settings_optional_notices),
                    subtitle = stringResource(R.string.settings_optional_notices_summary),
                    checked = state.optionalMessagesEnabled,
                    onCheckedChange = eventHandler::onOptionalMessagesEnabledChanged,
                ),
            ) + topicItems,
    )
}

private val NotificationTopic.titleRes: Int
    get() = when (this) {
        NotificationTopic.CLIPBOARD -> R.string.settings_topic_clipboard
        NotificationTopic.APP_LIFECYCLE -> R.string.settings_topic_app_lifecycle
        NotificationTopic.BACKUP -> R.string.settings_topic_backup
        NotificationTopic.SECURITY -> R.string.settings_topic_security
        NotificationTopic.DATABASE -> R.string.settings_topic_database
    }

private val NotificationTopic.summaryRes: Int
    get() = when (this) {
        NotificationTopic.CLIPBOARD -> R.string.settings_topic_clipboard_summary
        NotificationTopic.APP_LIFECYCLE -> R.string.settings_topic_app_lifecycle_summary
        NotificationTopic.BACKUP -> R.string.settings_topic_backup_summary
        NotificationTopic.SECURITY -> R.string.settings_topic_security_summary
        NotificationTopic.DATABASE -> R.string.settings_topic_database_summary
    }
