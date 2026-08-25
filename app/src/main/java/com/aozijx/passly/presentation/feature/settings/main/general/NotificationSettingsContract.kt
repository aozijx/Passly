package com.aozijx.passly.presentation.feature.settings.main.general

import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import com.aozijx.passly.presentation.ui.settings.general.NotificationSettingsUiModel
import com.aozijx.passly.presentation.ui.settings.general.NotificationTopic
import com.aozijx.passly.presentation.ui.settings.general.NotificationTopicUiModel

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

internal fun NotificationSettingsUiState.toUiModel() = NotificationSettingsUiModel(
    systemNotificationsEnabled = systemNotificationsEnabled,
    optionalMessagesEnabled = optionalMessagesEnabled,
    topics = MessageTopic.entries.map { topic ->
        NotificationTopicUiModel(
            topic = NotificationTopic.valueOf(topic.name),
            enabled = topicSetting(topic).enabled,
        )
    },
)

internal fun NotificationTopic.toFeatureModel() = MessageTopic.valueOf(name)

/** 通知设置页的一次性导航副作用（MVI）。 */
sealed interface NotificationSettingsEffect {
    data object OpenSystemNotificationSettings : NotificationSettingsEffect
}
