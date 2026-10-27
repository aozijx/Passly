package com.aozijx.passly.feature.settings.presentation.main.general

import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.feature.settings.presentation.ui.general.NotificationSettingsUiModel
import com.aozijx.passly.feature.settings.presentation.ui.general.NotificationTopicUiModel

sealed interface NotificationSettingsUiState {
    data object Loading : NotificationSettingsUiState

    data class Ready(
        val optionalMessagesEnabled: Boolean,
        val systemNotificationsEnabled: Boolean,
        val runtimeNotificationPermissionGranted: Boolean,
        val notificationsEnabledBySystem: Boolean,
        val notificationChannelEnabled: Boolean,
        val topicSettings: Map<MessageTopic, TopicMessageSettings>,
    ) : NotificationSettingsUiState {
        val systemNotificationAvailable: Boolean
            get() = runtimeNotificationPermissionGranted &&
                notificationsEnabledBySystem &&
                notificationChannelEnabled

        fun topicSetting(topic: MessageTopic): TopicMessageSettings =
            requireNotNull(topicSettings[topic]) { "Missing settings for $topic" }
    }
}

internal fun NotificationSettingsUiState.Ready.toUiModel() = NotificationSettingsUiModel(
    systemNotificationsEnabled = systemNotificationsEnabled,
    optionalMessagesEnabled = optionalMessagesEnabled,
    topics = MessageTopic.entries.map { topic ->
        NotificationTopicUiModel(
            topic = topic,
            enabled = topicSetting(topic).enabled,
        )
    },
)

internal sealed interface NotificationSettingsAction {
    data class SetSystemNotificationsEnabled(val enabled: Boolean) :
        NotificationSettingsAction

    data class RuntimePermissionGranted(val request: NotificationPermissionRequest) : NotificationSettingsAction
    data class RuntimePermissionDenied(val request: NotificationPermissionRequest) : NotificationSettingsAction
    data object CancelRuntimePermissionRequest : NotificationSettingsAction
    data object RefreshSystemNotificationState : NotificationSettingsAction
    data object OpenSystemNotificationSettings : NotificationSettingsAction
    data class SetOptionalMessagesEnabled(val enabled: Boolean) : NotificationSettingsAction
    data class SetTopicEnabled(val topic: MessageTopic, val enabled: Boolean) :
        NotificationSettingsAction
}

class NotificationPermissionRequest internal constructor()

/** 通知设置页的一次性导航与反馈。 */
sealed interface NotificationSettingsEffect {
    data class SaveFailed(val message: UiErrorMessage) : NotificationSettingsEffect
    data class RequestRuntimeNotificationPermission(val request: NotificationPermissionRequest) : NotificationSettingsEffect
    data object OpenSystemNotificationSettings : NotificationSettingsEffect
    data object ShowPermissionDenied : NotificationSettingsEffect
}
