package com.aozijx.passly.feature.settings.presentation.main.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.app.message.contract.SystemNotificationState
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.app.settings.SettingsReadRetrySignal
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.port.MessageSettingsRepository
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationSettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: MessageSettingsRepository,
        private val systemNotificationStateProvider: SystemNotificationStateProvider,
        settingsReadRetrySignal: SettingsReadRetrySignal = SettingsReadRetrySignal(),
    ) : ViewModel() {
        private data class PendingPermissionRequest(
            val request: NotificationPermissionRequest,
            val enableWhenGranted: Boolean,
            val owner: Any?,
        )

        private var pendingPermissionRequest: PendingPermissionRequest? = null
        private val messagesRead =
            RecoverableRead(
                source = settingsRepository.messages.map(MessageSettings::toUiModel),
                scope = viewModelScope,
                sharedRetrySignal = settingsReadRetrySignal,
            )

        private val pageEffects = PageEffects<NotificationSettingsEffect>()
        val effects = pageEffects.effects

        internal val uiState: StateFlow<RecoverableReadState<NotificationSettingsUiModel>> = messagesRead.state

        internal fun onAction(action: NotificationSettingsAction) {
            when (action) {
                NotificationSettingsAction.RetryRead -> {
                    messagesRead.retry()
                }

                is NotificationSettingsAction.SetSystemNotificationsEnabled -> {
                    if (action.enabled) {
                        requestSystemNotificationsEnabled()
                    } else {
                        pendingPermissionRequest = pendingPermissionRequest?.copy(enableWhenGranted = false)
                        persistSystemNotificationsEnabled(false)
                    }
                }

                is NotificationSettingsAction.RuntimePermissionGranted -> {
                    completeRuntimePermissionRequest(action.request)
                }

                is NotificationSettingsAction.RuntimePermissionDenied -> {
                    val pending = pendingPermissionRequest ?: return
                    if (pending.request !== action.request) return
                    pendingPermissionRequest = null
                    if (pending.enableWhenGranted) emitEffect(NotificationSettingsEffect.ShowPermissionDenied)
                }

                NotificationSettingsAction.OpenSystemNotificationSettings -> {
                    emitEffect(NotificationSettingsEffect.OpenSystemNotificationSettings)
                }

                is NotificationSettingsAction.SetOptionalMessagesEnabled -> {
                    saveSetting {
                        settingsRepository.setOptionalMessagesEnabled(action.enabled)
                    }
                }

                is NotificationSettingsAction.SetTopicEnabled -> {
                    saveSetting {
                        settingsRepository.setTopicEnabled(action.topic, action.enabled)
                    }
                }
            }
        }

        private fun requestSystemNotificationsEnabled() {
            pendingPermissionRequest?.let { pending ->
                pendingPermissionRequest = pending.copy(enableWhenGranted = true)
                return
            }
            val system = systemNotificationStateProvider.current()
            when {
                !system.runtimePermissionGranted -> {
                    val request = NotificationPermissionRequest()
                    pendingPermissionRequest =
                        PendingPermissionRequest(
                            request = request,
                            enableWhenGranted = true,
                            owner = pageEffects.currentOwner,
                        )
                    emitEffect(NotificationSettingsEffect.RequestRuntimeNotificationPermission(request))
                }

                system.isPlatformAvailable -> {
                    persistSystemNotificationsEnabled(true)
                }

                else -> {
                    reportPlatformUnavailable()
                }
            }
        }

        internal fun canLaunchRuntimePermissionRequest(request: NotificationPermissionRequest): Boolean {
            return pendingPermissionRequest?.let { pending ->
                pending.request === request &&
                    pending.enableWhenGranted &&
                    pending.owner?.let(pageEffects::isActive) == true
            } == true
        }

        private fun completeRuntimePermissionRequest(request: NotificationPermissionRequest) {
            val pending = pendingPermissionRequest ?: return
            if (pending.request !== request || pending.owner?.let(pageEffects::isActive) != true) return
            pendingPermissionRequest = null
            val system = systemNotificationStateProvider.current()
            if (!pending.enableWhenGranted) return
            if (system.isPlatformAvailable) {
                persistSystemNotificationsEnabled(true)
            } else {
                reportPlatformUnavailable()
            }
        }

        private fun persistSystemNotificationsEnabled(enabled: Boolean) =
            saveSetting {
                settingsRepository.setSystemNotificationsEnabled(enabled)
            }

        private fun saveSetting(write: suspend () -> Unit) {
            val owner = pageEffects.currentOwner ?: return
            viewModelScope.launch {
                if (uiState.value !is RecoverableReadState.Ready) return@launch
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
            pageEffects.tryEmit(effect)
        }

        private fun emitEffect(
            owner: Any,
            effect: NotificationSettingsEffect,
        ) {
            pageEffects.tryEmit(owner, effect)
        }

        fun onPageEntered(): Any {
            pageEffects.currentOwner?.let(::cancelPermissionRequest)
            val owner = pageEffects.enter()
            pendingPermissionRequest
                ?.takeIf { it.owner == null && it.enableWhenGranted }
                ?.let { pending ->
                    pendingPermissionRequest = pending.copy(owner = owner)
                    emitEffect(owner, NotificationSettingsEffect.RequestRuntimeNotificationPermission(pending.request))
                }
            return owner
        }

        fun onPageExited(
            owner: Any,
            preservePendingPermissionRequest: Boolean = false,
        ) {
            if (!pageEffects.isActive(owner)) return
            pendingPermissionRequest
                ?.takeIf { it.owner === owner }
                ?.let { pending ->
                    pendingPermissionRequest =
                        if (preservePendingPermissionRequest && pending.enableWhenGranted) {
                            pending.copy(owner = null)
                        } else {
                            null
                        }
                }
            pageEffects.exit(owner)
        }

        private fun cancelPermissionRequest(owner: Any) {
            pendingPermissionRequest
                ?.takeIf { it.owner === owner }
                ?.let { pendingPermissionRequest = null }
        }

    }

private val SystemNotificationState.isPlatformAvailable: Boolean
    get() = runtimePermissionGranted && notificationsEnabledBySystem && channelEnabled
