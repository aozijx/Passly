package com.aozijx.passly.presentation.ui.settings.general

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.group.RoundedGroup
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
    CLIPBOARD, APP_LIFECYCLE, ICON_DOWNLOAD, BACKUP, SECURITY, DATABASE,
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
    // ---- 系统通知总开关 ----
    SettingsSectionTitle(text = stringResource(R.string.settings_system_notifications))
    RoundedGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "notifications.system",
                title = stringResource(R.string.settings_system_notifications),
                subtitle = stringResource(R.string.settings_system_notifications_summary),
                checked = state.systemNotificationsEnabled,
                onCheckedChange = eventHandler::onSystemNotificationsEnabledChanged,
            ),
            navigationSettingsGroupItem(
                key = "notifications.system_settings",
                iconPlaceholder = true,
                title = stringResource(R.string.settings_system_notification_settings),
                subtitle = stringResource(R.string.settings_system_notification_settings_summary),
                onClick = eventHandler::onOpenSystemNotificationSettings,
            )
        )
    )

    Spacer(modifier = Modifier.height(24.dp))

    // ---- 应用内消息总开关 ----
    SettingsSectionTitle(text = stringResource(R.string.settings_app_notices))
    RoundedGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "notifications.optional_messages",
                title = stringResource(R.string.settings_optional_notices),
                subtitle = stringResource(R.string.settings_optional_notices_summary),
                checked = state.optionalMessagesEnabled,
                onCheckedChange = eventHandler::onOptionalMessagesEnabledChanged,
            )
        )
    )

    // ---- 话题分类开关（总开关开启时才显示） ----
    if (state.optionalMessagesEnabled) {
        Spacer(modifier = Modifier.height(8.dp))

        state.topics.forEach { item ->
            val topic = item.topic
            RoundedGroup(
                items = listOf(
                    switchSettingsGroupItem(
                        key = "notifications.topic.${topic.name}",
                        title = stringResource(topic.titleRes),
                        subtitle = stringResource(topic.summaryRes),
                        checked = item.enabled,
                        onCheckedChange = { enabled ->
                            eventHandler.onTopicEnabledChanged(topic, enabled)
                        },
                    )
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private val NotificationTopic.titleRes: Int
    get() = when (this) {
        NotificationTopic.CLIPBOARD -> R.string.settings_topic_clipboard
        NotificationTopic.APP_LIFECYCLE -> R.string.settings_topic_app_lifecycle
        NotificationTopic.ICON_DOWNLOAD -> R.string.settings_topic_icon_download
        NotificationTopic.BACKUP -> R.string.settings_topic_backup
        NotificationTopic.SECURITY -> R.string.settings_topic_security
        NotificationTopic.DATABASE -> R.string.settings_topic_database
    }

private val NotificationTopic.summaryRes: Int
    get() = when (this) {
        NotificationTopic.CLIPBOARD -> R.string.settings_topic_clipboard_summary
        NotificationTopic.APP_LIFECYCLE -> R.string.settings_topic_app_lifecycle_summary
        NotificationTopic.ICON_DOWNLOAD -> R.string.settings_topic_icon_download_summary
        NotificationTopic.BACKUP -> R.string.settings_topic_backup_summary
        NotificationTopic.SECURITY -> R.string.settings_topic_security_summary
        NotificationTopic.DATABASE -> R.string.settings_topic_database_summary
    }
