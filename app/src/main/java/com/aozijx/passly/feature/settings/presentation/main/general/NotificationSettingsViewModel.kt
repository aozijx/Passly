package com.aozijx.passly.feature.settings.presentation.main.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.app.message.contract.SystemNotificationState
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import com.aozijx.passly.domain.settings.port.MessageSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settingsRepository: MessageSettingsRepository,
    private val systemNotificationStateProvider: SystemNotificationStateProvider
) : ViewModel() {
    private sealed interface PermissionRequestState {
        data object Idle : PermissionRequestState
        data class Pending(val request: NotificationPermissionRequest, val enableWhenGranted: Boolean) : PermissionRequestState
    }
    private var permissionRequestState: PermissionRequestState = PermissionRequestState.Idle
    private val systemNotificationState = MutableStateFlow(systemNotificationStateProvider.current())

    private data class PageEffect(val owner: Any, val effect: NotificationSettingsEffect)

    private val _effects = Channel<PageEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
        .filter { activePageOwner === it.owner }
        .map { it.effect }

    private var activePageOwner: Any? = null

    val uiState: StateFlow<NotificationSettingsUiState> = settingsRepository.messages
        .combine(systemNotificationState) { messages, system ->
            NotificationSettingsUiState.Ready(
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
            initialValue = NotificationSettingsUiState.Loading
        )

    internal fun onAction(action: NotificationSettingsAction) {
        when (action) {
            is NotificationSettingsAction.SetSystemNotificationsEnabled -> {
                if (action.enabled) requestSystemNotificationsEnabled()
                else {
                    val pending = permissionRequestState
                    if (pending is PermissionRequestState.Pending) {
                        permissionRequestState = pending.copy(enableWhenGranted = false)
                    }
                    persistSystemNotificationsEnabled(false)
                }
            }

            is NotificationSettingsAction.RuntimePermissionGranted ->
                completeRuntimePermissionRequest(action.request)

            is NotificationSettingsAction.RuntimePermissionDenied -> {
                val pending = permissionRequestState as? PermissionRequestState.Pending ?: return
                if (pending.request !== action.request) return
                permissionRequestState = PermissionRequestState.Idle
                readSystemNotificationState()
                if (pending.enableWhenGranted) emitEffect(NotificationSettingsEffect.ShowPermissionDenied)
            }

            NotificationSettingsAction.CancelRuntimePermissionRequest ->
                permissionRequestState = PermissionRequestState.Idle

            NotificationSettingsAction.RefreshSystemNotificationState ->
                readSystemNotificationState()

            NotificationSettingsAction.OpenSystemNotificationSettings ->
                emitEffect(NotificationSettingsEffect.OpenSystemNotificationSettings)

            is NotificationSettingsAction.SetOptionalMessagesEnabled -> saveSetting {
                settingsRepository.setOptionalMessagesEnabled(action.enabled)
            }

            is NotificationSettingsAction.SetTopicEnabled -> saveSetting {
                settingsRepository.setTopicEnabled(action.topic, action.enabled)
            }
        }
    }

    private fun requestSystemNotificationsEnabled() {
        val pending = permissionRequestState
        if (pending is PermissionRequestState.Pending) {
            permissionRequestState = pending.copy(enableWhenGranted = true)
            return
        }
        val system = readSystemNotificationState()
        when {
            !system.runtimePermissionGranted -> {
                val request = NotificationPermissionRequest()
                permissionRequestState = PermissionRequestState.Pending(request, enableWhenGranted = true)
                emitEffect(NotificationSettingsEffect.RequestRuntimeNotificationPermission(request))
            }

            system.isPlatformAvailable -> persistSystemNotificationsEnabled(true)
            else -> reportPlatformUnavailable()
        }
    }

    internal fun canLaunchRuntimePermissionRequest(request: NotificationPermissionRequest): Boolean {
        val pending = permissionRequestState
        return pending is PermissionRequestState.Pending && pending.request === request && pending.enableWhenGranted
    }

    private fun completeRuntimePermissionRequest(request: NotificationPermissionRequest) {
        val pending = permissionRequestState as? PermissionRequestState.Pending ?: return
        if (pending.request !== request) return
        permissionRequestState = PermissionRequestState.Idle
        val system = readSystemNotificationState()
        if (!pending.enableWhenGranted) return
        if (system.isPlatformAvailable) persistSystemNotificationsEnabled(true)
        else reportPlatformUnavailable()
    }

    private fun persistSystemNotificationsEnabled(enabled: Boolean) = saveSetting {
        settingsRepository.setSystemNotificationsEnabled(enabled)
    }

    private fun saveSetting(write: suspend () -> Unit) {
        val owner = activePageOwner ?: return
        viewModelScope.launch {
            writePreferenceWithFeedback(
                failureRes = R.string.settings_notifications_save_failed,
                onFailure = { emitEffect(owner, NotificationSettingsEffect.SaveFailed(it)) },
                write = write,
            )
        }
    }

    private fun reportPlatformUnavailable() {
        emitEffect(NotificationSettingsEffect.ShowPermissionDenied)
        emitEffect(NotificationSettingsEffect.OpenSystemNotificationSettings)
    }

    private fun emitEffect(effect: NotificationSettingsEffect) {
        activePageOwner?.let { emitEffect(it, effect) }
    }

    private fun emitEffect(owner: Any, effect: NotificationSettingsEffect) {
        if (activePageOwner === owner) _effects.trySend(PageEffect(owner, effect))
    }

    fun onPageEntered(): Any {
        val owner = Any()
        activePageOwner = owner
        return owner
    }

    fun onPageExited(owner: Any) {
        if (activePageOwner === owner) activePageOwner = null
    }

    private fun readSystemNotificationState() =
        systemNotificationStateProvider.current().also { systemNotificationState.value = it }
}

private val SystemNotificationState.isPlatformAvailable: Boolean
    get() = runtimePermissionGranted && notificationsEnabledBySystem && channelEnabled
