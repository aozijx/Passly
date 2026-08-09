package com.aozijx.passly.feature.settings.general

import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.model.NoticeTopic

sealed interface NotificationSettingsIntent {
    data object RefreshSystemNotificationState : NotificationSettingsIntent
    data class SetOptionalMessagesEnabled(val enabled: Boolean) : NotificationSettingsIntent
    data class SetSystemNotificationsEnabled(val enabled: Boolean) : NotificationSettingsIntent
    data class SetMessageTopicEnabled(val topic: NoticeTopic, val enabled: Boolean) :
        NotificationSettingsIntent

    data class SetMessageTopicMinimumLevel(val topic: NoticeTopic, val level: NoticeLevel) :
        NotificationSettingsIntent
}