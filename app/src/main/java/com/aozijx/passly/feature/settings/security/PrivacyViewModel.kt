package com.aozijx.passly.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyUiState(
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = false,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val reauthenticateSensitiveCopies: Boolean = true,
)

sealed interface PrivacyUiAction {
    data class SetSecureContentEnabled(val enabled: Boolean) : PrivacyUiAction
    data class SetFlipToLockEnabled(val enabled: Boolean) : PrivacyUiAction
    data class SetFlipExitAndClearStackEnabled(val enabled: Boolean) : PrivacyUiAction
    data class SetSensitiveCopyReauthentication(val enabled: Boolean) : PrivacyUiAction
}

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<PrivacyUiState> = combine(
        settingsRepository.settings.map { it.security.isSecureContentEnabled },
        settingsRepository.settings.map { it.security.isFlipToLockEnabled },
        settingsRepository.settings.map { it.security.isFlipExitAndClearStackEnabled },
        settingsRepository.settings.map { it.security.reauthenticateSensitiveCopies }
    ) { sec, ftl, fec, copyAuth ->
        PrivacyUiState(
            isSecureContentEnabled = sec,
            isFlipToLockEnabled = ftl,
            isFlipExitAndClearStackEnabled = fec,
            reauthenticateSensitiveCopies = copyAuth,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        PrivacyUiState()
    )

    fun onAction(action: PrivacyUiAction) {
        when (action) {
            is PrivacyUiAction.SetSecureContentEnabled -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetSecureContentEnabled(action.enabled))
            }

            is PrivacyUiAction.SetFlipToLockEnabled -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetFlipToLockEnabled(action.enabled))
            }

            is PrivacyUiAction.SetFlipExitAndClearStackEnabled -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetFlipExitAndClearStackEnabled(action.enabled))
            }

            is PrivacyUiAction.SetSensitiveCopyReauthentication -> viewModelScope.launch {
                settingsRepository.update(
                    SettingsCommand.SetReauthenticateSensitiveCopies(action.enabled)
                )
            }
        }
    }
}
