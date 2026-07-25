package com.aozijx.passly.feature.settings.general

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.notice.model.NoticeTopic

@Composable
internal fun NotificationSettingsSection(
    state: NotificationSettingsUiState,
    onSystemNotificationsEnabledChange: (Boolean) -> Unit,
    onOptionalMessagesEnabledChange: (Boolean) -> Unit = {},
    onTopicEnabledChange: (NoticeTopic, Boolean) -> Unit = { _, _ -> }
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
                onCheckedChange = onSystemNotificationsEnabledChange
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
                onCheckedChange = onOptionalMessagesEnabledChange
            )
        )
    )

    // ---- 话题分类开关（总开关开启时才显示） ----
    if (state.optionalMessagesEnabled) {
        Spacer(modifier = Modifier.height(8.dp))

        val topicLabels = mapOf(
            NoticeTopic.CLIPBOARD to R.string.settings_topic_clipboard,
            NoticeTopic.APP_LIFECYCLE to R.string.settings_topic_app_lifecycle,
            NoticeTopic.ICON_DOWNLOAD to R.string.settings_topic_icon_download,
            NoticeTopic.BACKUP to R.string.settings_topic_backup,
            NoticeTopic.SECURITY to R.string.settings_topic_security,
            NoticeTopic.DATABASE to R.string.settings_topic_database
        )
        val topicSummaries = mapOf(
            NoticeTopic.CLIPBOARD to R.string.settings_topic_clipboard_summary,
            NoticeTopic.APP_LIFECYCLE to R.string.settings_topic_app_lifecycle_summary,
            NoticeTopic.ICON_DOWNLOAD to R.string.settings_topic_icon_download_summary,
            NoticeTopic.BACKUP to R.string.settings_topic_backup_summary,
            NoticeTopic.SECURITY to R.string.settings_topic_security_summary,
            NoticeTopic.DATABASE to R.string.settings_topic_database_summary
        )

        NoticeTopic.entries.forEach { topic ->
            val setting = state.topicSetting(topic)
            RoundedGroup(
                items = listOf(
                    switchSettingsGroupItem(
                        key = "notifications.topic.${topic.name}",
                        title = stringResource(topicLabels.getValue(topic)),
                        subtitle = stringResource(topicSummaries.getValue(topic)),
                        checked = setting.enabled,
                        onCheckedChange = { enabled -> onTopicEnabledChange(topic, enabled) }
                    )
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
