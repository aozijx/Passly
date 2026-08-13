package com.aozijx.passly.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.data.settings.model.SettingsCommand
import com.aozijx.passly.data.settings.port.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<PrivacySettingsUiState> = combine(
        settingsRepository.settings.map { it.security.isSecureContentEnabled },
        settingsRepository.settings.map { it.security.isFlipToLockEnabled },
        settingsRepository.settings.map { it.security.isFlipExitAndClearStackEnabled },
        settingsRepository.settings.map { it.security.reauthenticateSensitiveCopies }
    ) { sec, ftl, fec, copyAuth ->
        PrivacySettingsUiState(
            isSecureContentEnabled = sec,
            isFlipToLockEnabled = ftl,
            isFlipExitAndClearStackEnabled = fec,
            reauthenticateSensitiveCopies = copyAuth,
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
        }
    }
}
