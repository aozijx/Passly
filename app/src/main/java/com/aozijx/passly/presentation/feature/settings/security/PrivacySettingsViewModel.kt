package com.aozijx.passly.presentation.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.clipboard.ClipboardCopyController
import com.aozijx.passly.core.platform.clipboard.ClipboardClearResult
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val clipboardCopyController: ClipboardCopyController,
) : ViewModel() {

    private val _effects = Channel<PrivacySettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<PrivacySettingsUiState> = settingsRepository.settings.map {
        val security = it.security
        PrivacySettingsUiState(
            isSecureContentEnabled = security.isSecureContentEnabled,
            isFlipToLockEnabled = security.isFlipToLockEnabled,
            isFlipExitAndClearStackEnabled = security.isFlipExitAndClearStackEnabled,
            reauthenticateSensitiveCopies = security.reauthenticateSensitiveCopies,
            clipboardClearEnabled = security.clipboardClearPolicy.enabled,
            clipboardClearDelaySeconds = security.clipboardClearPolicy.delaySeconds,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        PrivacySettingsUiState()
    )

    fun onAction(action: PrivacySettingsAction) {
        when (action) {
            is PrivacySettingsAction.SetSecureContentEnabled -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetSecureContentEnabled(action.enabled))
            }

            is PrivacySettingsAction.SetFlipToLockEnabled -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetFlipToLockEnabled(action.enabled))
            }

            is PrivacySettingsAction.SetFlipExitAndClearStackEnabled -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetFlipExitAndClearStackEnabled(action.enabled))
            }

            is PrivacySettingsAction.SetSensitiveCopyReauthentication -> viewModelScope.launch {
                settingsRepository.update(
                    SettingsCommand.SetReauthenticateSensitiveCopies(action.enabled)
                )
            }

            is PrivacySettingsAction.SetClipboardClearEnabled -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetClipboardClearEnabled(action.enabled))
            }

            is PrivacySettingsAction.SetClipboardClearDelaySeconds -> viewModelScope.launch {
                settingsRepository.update(
                    SettingsCommand.SetClipboardClearDelaySeconds(action.seconds)
                )
            }

            PrivacySettingsAction.ClearClipboardNow -> {
                val effect = when (clipboardCopyController.clearOwned()) {
                    ClipboardClearResult.Cleared -> PrivacySettingsEffect.ClipboardCleared
                    else -> PrivacySettingsEffect.ClipboardNotCleared
                }
                _effects.trySend(effect)
            }
        }
    }
}
