package com.aozijx.passly.feature.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val systemNotificationStateProvider: SystemNotificationStateProvider
) : ViewModel() {
    private val systemNotificationState = MutableStateFlow(systemNotificationStateProvider.current())

    private val _effects = Channel<NotificationSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

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

    fun refreshSystemNotificationState() {
        readSystemNotificationState()
    }

    fun openSystemNotificationSettings() {
        _effects.trySend(NotificationSettingsEffect.OpenSystemNotificationSettings)
    }

    fun systemNotificationsAvailableNow(): Boolean {
        val system = readSystemNotificationState()
        return system.runtimePermissionGranted &&
            system.notificationsEnabledBySystem &&
            system.channelEnabled
    }

    fun setOptionalMessagesEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetOptionalMessagesEnabled(enabled))
    }

    fun setSystemNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetSystemNotificationsEnabled(enabled))
    }

    fun setMessageTopicEnabled(topic: MessageTopic, enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetMessageTopicEnabled(topic, enabled))
    }

    fun setMessageTopicMinimumLevel(topic: MessageTopic, level: MessageLevel) =
        viewModelScope.launch {
            settingsRepository.update(SettingsCommand.SetMessageTopicMinimumLevel(topic, level))
        }

    private fun readSystemNotificationState() =
        systemNotificationStateProvider.current().also { systemNotificationState.value = it }
}
