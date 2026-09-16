package com.aozijx.passly.presentation.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.clipboard.port.OwnedClipboardCleaner
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
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
    private val settingsRepository: SecuritySettingsRepository,
    private val clipboardCleaner: OwnedClipboardCleaner,
) : ViewModel() {

    private val _effects = Channel<PrivacySettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<PrivacySettingsUiState> = settingsRepository.security.map { security ->
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
                settingsRepository.setSecureContentEnabled(action.enabled)
            }

            is PrivacySettingsAction.SetFlipToLockEnabled -> viewModelScope.launch {
                settingsRepository.setFlipToLockEnabled(action.enabled)
            }

            is PrivacySettingsAction.SetFlipExitAndClearStackEnabled -> viewModelScope.launch {
                settingsRepository.setFlipExitAndClearStackEnabled(action.enabled)
            }

            is PrivacySettingsAction.SetSensitiveCopyReauthentication -> viewModelScope.launch {
                settingsRepository.setReauthenticateSensitiveCopies(action.enabled)
            }

            is PrivacySettingsAction.SetClipboardClearEnabled -> viewModelScope.launch {
                settingsRepository.setClipboardClearEnabled(action.enabled)
            }

            is PrivacySettingsAction.SetClipboardClearDelaySeconds -> viewModelScope.launch {
                settingsRepository.setClipboardClearDelaySeconds(action.seconds)
            }

            PrivacySettingsAction.ClearClipboardNow -> {
                val effect = if (clipboardCleaner.clearOwned()) {
                    PrivacySettingsEffect.ClipboardCleared
                } else {
                    PrivacySettingsEffect.ClipboardNotCleared
                }
                _effects.trySend(effect)
            }
        }
    }
}
