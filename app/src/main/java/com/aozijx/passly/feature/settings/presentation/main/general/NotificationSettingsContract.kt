package com.aozijx.passly.feature.settings.presentation.main.general

import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import com.aozijx.passly.presentation.shared.error.UiErrorMessage

sealed interface NotificationSettingsUiState {
    data object Loading : NotificationSettingsUiState

    data class Failed(
        val failure: ReadFailure,
    ) : NotificationSettingsUiState

    data class Ready(
        val optionalMessagesEnabled: Boolean,
        val systemNotificationsEnabled: Boolean,
        val runtimeNotificationPermissionGranted: Boolean,
        val notificationsEnabledBySystem: Boolean,
        val notificationChannelEnabled: Boolean,
        val topicSettings: Map<MessageTopic, TopicMessageSettings>,
    ) : NotificationSettingsUiState {
    }
}

internal data class NotificationSettingsUiModel(
    val systemNotificationsEnabled: Boolean,
    val optionalMessagesEnabled: Boolean,
    val topics: List<NotificationTopicUiModel>,
)

internal data class NotificationTopicUiModel(
    val topic: MessageTopic,
    val enabled: Boolean,
)

internal fun NotificationSettingsUiState.Ready.toUiModel() =
    NotificationSettingsUiModel(
        systemNotificationsEnabled = systemNotificationsEnabled,
        optionalMessagesEnabled = optionalMessagesEnabled,
        topics =
            MessageTopic.entries.map { topic ->
                NotificationTopicUiModel(
                    topic = topic,
                    enabled = (topicSettings[topic] ?: TopicMessageSettings()).enabled,
                )
            },
    )

internal sealed interface NotificationSettingsAction {
    data object RetryRead : NotificationSettingsAction

    data class SetSystemNotificationsEnabled(
        val enabled: Boolean,
    ) : NotificationSettingsAction

    data class RuntimePermissionGranted(
        val request: NotificationPermissionRequest,
    ) : NotificationSettingsAction

    data class RuntimePermissionDenied(
        val request: NotificationPermissionRequest,
    ) : NotificationSettingsAction

    data object RefreshSystemNotificationState : NotificationSettingsAction

    data object OpenSystemNotificationSettings : NotificationSettingsAction

    data class SetOptionalMessagesEnabled(
        val enabled: Boolean,
    ) : NotificationSettingsAction

    data class SetTopicEnabled(
        val topic: MessageTopic,
        val enabled: Boolean,
    ) : NotificationSettingsAction
}

class NotificationPermissionRequest internal constructor()

/** 通知设置页的一次性导航与反馈。 */
sealed interface NotificationSettingsEffect {
    data class SaveFailed(
        val message: UiErrorMessage,
    ) : NotificationSettingsEffect

    data class RequestRuntimeNotificationPermission(
        val request: NotificationPermissionRequest,
    ) : NotificationSettingsEffect

    data object OpenSystemNotificationSettings : NotificationSettingsEffect

    data object ShowPermissionDenied : NotificationSettingsEffect
}
