package com.aozijx.passly.feature.settings.general

import com.aozijx.passly.data.message.model.NoticeTopic
import com.aozijx.passly.data.message.model.TopicMessageSettings

data class NotificationSettingsUiState(
    val optionalMessagesEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val runtimeNotificationPermissionGranted: Boolean = true,
    val notificationsEnabledBySystem: Boolean = true,
    val notificationChannelEnabled: Boolean = true,
    val topicSettings: Map<NoticeTopic, TopicMessageSettings> = emptyMap()
) {
    val systemNotificationAvailable: Boolean
        get() = runtimeNotificationPermissionGranted &&
            notificationsEnabledBySystem &&
            notificationChannelEnabled

    fun topicSetting(topic: NoticeTopic): TopicMessageSettings =
        topicSettings[topic] ?: TopicMessageSettings()
}
