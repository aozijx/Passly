package com.aozijx.passly.feature.settings.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
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
    private var activePageOwner: Any? = null

    val uiState: StateFlow<PrivacySettingsUiState> = settingsRepository.security.map { security ->
        PrivacySettingsUiState.Ready(
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
        PrivacySettingsUiState.Loading
    )

    fun onAction(action: PrivacySettingsAction) {
        when (action) {
            is PrivacySettingsAction.SetSecureContentEnabled -> saveSetting {
                settingsRepository.setSecureContentEnabled(action.enabled)
            }

            is PrivacySettingsAction.SetFlipToLockEnabled -> saveSetting {
                settingsRepository.setFlipToLockEnabled(action.enabled)
            }

            is PrivacySettingsAction.SetFlipExitAndClearStackEnabled -> saveSetting {
                settingsRepository.setFlipExitAndClearStackEnabled(action.enabled)
            }

            is PrivacySettingsAction.SetSensitiveCopyReauthentication -> saveSetting {
                settingsRepository.setReauthenticateSensitiveCopies(action.enabled)
            }

            is PrivacySettingsAction.SetClipboardClearEnabled -> saveSetting {
                settingsRepository.setClipboardClearEnabled(action.enabled)
            }

            is PrivacySettingsAction.SetClipboardClearDelaySeconds -> saveSetting {
                settingsRepository.setClipboardClearDelaySeconds(action.seconds)
            }

            PrivacySettingsAction.ClearClipboardNow -> {
                val owner = activePageOwner ?: return
                val effect = if (clipboardCleaner.clearOwned()) {
                    PrivacySettingsEffect.ClipboardCleared
                } else {
                    PrivacySettingsEffect.ClipboardNotCleared
                }
                emitEffect(owner, effect)
            }
        }
    }

    private fun saveSetting(write: suspend () -> Unit) {
        val owner = activePageOwner ?: return
        viewModelScope.launch {
            writePreferenceWithFeedback(
                failureRes = R.string.settings_privacy_save_failed,
                onFailure = { emitEffect(owner, PrivacySettingsEffect.SaveFailed(it)) },
                write = write,
            )
        }
    }

    fun onPageEntered(): Any {
        val owner = Any()
        activePageOwner = owner
        return owner
    }

    fun onPageExited(owner: Any) {
        if (activePageOwner === owner) activePageOwner = null
    }

    private fun emitEffect(owner: Any, effect: PrivacySettingsEffect) {
        if (activePageOwner === owner) _effects.trySend(effect)
    }
}
