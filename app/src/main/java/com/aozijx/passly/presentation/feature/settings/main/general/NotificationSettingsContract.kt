package com.aozijx.passly.presentation.feature.settings.main.general

import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import com.aozijx.passly.presentation.feature.settings.ui.general.NotificationSettingsEvent
import com.aozijx.passly.presentation.feature.settings.ui.general.NotificationSettingsUiModel
import com.aozijx.passly.presentation.feature.settings.ui.general.NotificationTopic
import com.aozijx.passly.presentation.feature.settings.ui.general.NotificationTopicUiModel

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
            topic = topic.toUiModel(),
            enabled = topicSetting(topic).enabled,
        )
    },
)

internal fun MessageTopic.toUiModel(): NotificationTopic = when (this) {
    MessageTopic.CLIPBOARD -> NotificationTopic.CLIPBOARD
    MessageTopic.APP_LIFECYCLE -> NotificationTopic.APP_LIFECYCLE
    MessageTopic.BACKUP -> NotificationTopic.BACKUP
    MessageTopic.SECURITY -> NotificationTopic.SECURITY
    MessageTopic.DATABASE -> NotificationTopic.DATABASE
}

internal fun NotificationTopic.toFeatureModel(): MessageTopic = when (this) {
    NotificationTopic.CLIPBOARD -> MessageTopic.CLIPBOARD
    NotificationTopic.APP_LIFECYCLE -> MessageTopic.APP_LIFECYCLE
    NotificationTopic.BACKUP -> MessageTopic.BACKUP
    NotificationTopic.SECURITY -> MessageTopic.SECURITY
    NotificationTopic.DATABASE -> MessageTopic.DATABASE
}

internal sealed interface NotificationSettingsAction {
    data class SetSystemNotificationsEnabled(val enabled: Boolean) :
        NotificationSettingsAction

    data object RuntimePermissionGranted : NotificationSettingsAction
    data object RuntimePermissionDenied : NotificationSettingsAction
    data object RefreshSystemNotificationState : NotificationSettingsAction
    data object OpenSystemNotificationSettings : NotificationSettingsAction
    data class SetOptionalMessagesEnabled(val enabled: Boolean) : NotificationSettingsAction
    data class SetTopicEnabled(val topic: MessageTopic, val enabled: Boolean) :
        NotificationSettingsAction
}

internal fun NotificationSettingsEvent.toAction(): NotificationSettingsAction = when (this) {
    is NotificationSettingsEvent.SystemNotificationsEnabledChanged ->
        NotificationSettingsAction.SetSystemNotificationsEnabled(enabled)

    NotificationSettingsEvent.OpenSystemNotificationSettings ->
        NotificationSettingsAction.OpenSystemNotificationSettings

    is NotificationSettingsEvent.OptionalMessagesEnabledChanged ->
        NotificationSettingsAction.SetOptionalMessagesEnabled(enabled)

    is NotificationSettingsEvent.TopicEnabledChanged ->
        NotificationSettingsAction.SetTopicEnabled(topic.toFeatureModel(), enabled)
}

/** 通知设置页的一次性导航副作用（MVI）。 */
sealed interface NotificationSettingsEffect {
    data object RequestRuntimeNotificationPermission : NotificationSettingsEffect
    data object OpenSystemNotificationSettings : NotificationSettingsEffect
    data object ShowPermissionDenied : NotificationSettingsEffect
}
