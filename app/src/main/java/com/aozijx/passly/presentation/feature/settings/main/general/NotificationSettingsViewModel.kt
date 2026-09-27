package com.aozijx.passly.presentation.feature.settings.main.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.contract.SystemNotificationState
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import com.aozijx.passly.domain.settings.port.MessageSettingsRepository
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
    private val settingsRepository: MessageSettingsRepository,
    private val systemNotificationStateProvider: SystemNotificationStateProvider
) : ViewModel() {
    private val systemNotificationState = MutableStateFlow(systemNotificationStateProvider.current())

    private val _effects = Channel<NotificationSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<NotificationSettingsUiState> = settingsRepository.messages
        .combine(systemNotificationState) { messages, system ->
            NotificationSettingsUiState(
                optionalMessagesEnabled = messages.optionalMessagesEnabled,
                systemNotificationsEnabled = messages.systemNotificationsEnabled,
                runtimeNotificationPermissionGranted = system.runtimePermissionGranted,
                notificationsEnabledBySystem = system.notificationsEnabledBySystem,
                notificationChannelEnabled = system.channelEnabled,
                topicSettings = messages.topicSettings
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationSettingsUiState()
        )

    internal fun onAction(action: NotificationSettingsAction) {
        when (action) {
            is NotificationSettingsAction.SetSystemNotificationsEnabled -> {
                if (action.enabled) requestSystemNotificationsEnabled()
                else persistSystemNotificationsEnabled(false)
            }

            NotificationSettingsAction.RuntimePermissionGranted ->
                completeRuntimePermissionRequest()

            NotificationSettingsAction.RuntimePermissionDenied ->
                emitEffect(NotificationSettingsEffect.ShowPermissionDenied)

            NotificationSettingsAction.RefreshSystemNotificationState ->
                readSystemNotificationState()

            NotificationSettingsAction.OpenSystemNotificationSettings ->
                emitEffect(NotificationSettingsEffect.OpenSystemNotificationSettings)

            is NotificationSettingsAction.SetOptionalMessagesEnabled -> viewModelScope.launch {
                settingsRepository.setOptionalMessagesEnabled(action.enabled)
            }

            is NotificationSettingsAction.SetTopicEnabled -> viewModelScope.launch {
                settingsRepository.setTopicEnabled(action.topic, action.enabled)
            }
        }
    }

    private fun requestSystemNotificationsEnabled() {
        val system = readSystemNotificationState()
        when {
            !system.runtimePermissionGranted ->
                emitEffect(NotificationSettingsEffect.RequestRuntimeNotificationPermission)

            system.isPlatformAvailable -> persistSystemNotificationsEnabled(true)
            else -> reportPlatformUnavailable()
        }
    }

    private fun completeRuntimePermissionRequest() {
        val system = readSystemNotificationState()
        if (system.isPlatformAvailable) persistSystemNotificationsEnabled(true)
        else reportPlatformUnavailable()
    }

    private fun persistSystemNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setSystemNotificationsEnabled(enabled)
    }

    private fun reportPlatformUnavailable() {
        emitEffect(NotificationSettingsEffect.ShowPermissionDenied)
        emitEffect(NotificationSettingsEffect.OpenSystemNotificationSettings)
    }

    private fun emitEffect(effect: NotificationSettingsEffect) {
        _effects.trySend(effect)
    }

    private fun readSystemNotificationState() =
        systemNotificationStateProvider.current().also { systemNotificationState.value = it }
}

private val SystemNotificationState.isPlatformAvailable: Boolean
    get() = runtimePermissionGranted && notificationsEnabledBySystem && channelEnabled
