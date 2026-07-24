package com.aozijx.passly.feature.settings.general

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

data class NotificationSettingsUiState(
    val statusBarEnabled: Boolean = false,
    val iconDownloadsEnabled: Boolean = true
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {
    val uiState: StateFlow<NotificationSettingsUiState> = combine(
        settingsRepository.settings.map { it.notifications.statusBarNotificationsEnabled },
        settingsRepository.settings.map { it.notifications.iconDownloadNotificationsEnabled }
    ) { statusBar, iconDownloads ->
        NotificationSettingsUiState(statusBar, iconDownloads)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationSettingsUiState()
    )

    fun setStatusBarEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetStatusBarNotificationsEnabled(enabled))
    }

    fun setIconDownloadsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetIconDownloadNotificationsEnabled(enabled))
    }
}
