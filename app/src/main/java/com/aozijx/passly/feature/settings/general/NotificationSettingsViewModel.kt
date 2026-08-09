package com.aozijx.passly.feature.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.model.NoticeTopic
import com.aozijx.passly.domain.notice.port.SystemNotificationStateProvider
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val systemNotificationStateProvider: SystemNotificationStateProvider
) : ViewModel() {

    private val systemNotificationState = MutableStateFlow(systemNotificationStateProvider.current())

    val uiState: StateFlow<NotificationSettingsUiState> = settingsRepository.settings
        .combine(systemNotificationState) { s, system ->
            val n = s.messages
            NotificationSettingsUiState(
                optionalMessagesEnabled = n.optionalMessagesEnabled,
                systemNotificationsEnabled = n.systemNotificationsEnabled,
                runtimeNotificationPermissionGranted = system.runtimePermissionGranted,
                notificationsEnabledBySystem = system.notificationsEnabledBySystem,
                notificationChannelEnabled = system.channelEnabled,
                topicSettings = n.topicSettings
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationSettingsUiState()
        )

    fun onIntent(intent: NotificationSettingsIntent) {
        when (intent) {
            NotificationSettingsIntent.RefreshSystemNotificationState -> refreshSystemNotificationState()
            is NotificationSettingsIntent.SetOptionalMessagesEnabled -> setOptionalMessagesEnabled(
                intent.enabled
            )

            is NotificationSettingsIntent.SetSystemNotificationsEnabled -> setSystemNotificationsEnabled(
                intent.enabled
            )

            is NotificationSettingsIntent.SetMessageTopicEnabled -> setMessageTopicEnabled(
                intent.topic,
                intent.enabled
            )

            is NotificationSettingsIntent.SetMessageTopicMinimumLevel -> setMessageTopicMinimumLevel(
                intent.topic,
                intent.level
            )
        }
    }

    fun systemNotificationsAvailableNow(): Boolean {
        val system = readSystemNotificationState()
        return system.runtimePermissionGranted &&
            system.notificationsEnabledBySystem &&
            system.channelEnabled
    }

    private fun refreshSystemNotificationState() {
        readSystemNotificationState()
    }

    private fun setOptionalMessagesEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetOptionalMessagesEnabled(enabled))
    }

    private fun setSystemNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetSystemNotificationsEnabled(enabled))
    }

    private fun setMessageTopicEnabled(topic: NoticeTopic, enabled: Boolean) =
        viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetMessageTopicEnabled(topic, enabled))
    }

    private fun setMessageTopicMinimumLevel(topic: NoticeTopic, level: NoticeLevel) =
        viewModelScope.launch {
            settingsRepository.update(SettingsCommand.SetMessageTopicMinimumLevel(topic, level))
        }

    private fun readSystemNotificationState() =
        systemNotificationStateProvider.current().also { systemNotificationState.value = it }
}