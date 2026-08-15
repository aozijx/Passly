package com.aozijx.passly.feature.settings.general

import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings

data class NotificationSettingsUiState(
    val optionalMessagesEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val runtimeNotificationPermissionGranted: Boolean = true,
    val notificationsEnabledBySystem: Boolean = true,
    val notificationChannelEnabled: Boolean = true,
    val topicSettings: Map<MessageTopic, TopicMessageSettings> = emptyMap()
) {
    val systemNotificationAvailable: Boolean
        get() = runtimeNotificationPermissionGranted &&
            notificationsEnabledBySystem &&
            notificationChannelEnabled

    fun topicSetting(topic: MessageTopic): TopicMessageSettings =
        topicSettings[topic] ?: TopicMessageSettings()
}
