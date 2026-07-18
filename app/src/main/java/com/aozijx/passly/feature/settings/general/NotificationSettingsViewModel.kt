package com.aozijx.passly.feature.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.usecase.settings.PortableSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val statusBarEnabled: Boolean = true,
    val iconDownloadsEnabled: Boolean = true
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settings: PortableSettingsUseCases
) : ViewModel() {
    val uiState: StateFlow<NotificationSettingsUiState> = combine(
        settings.statusBarNotificationsEnabled,
        settings.iconDownloadNotificationsEnabled
    ) { statusBar, iconDownloads ->
        NotificationSettingsUiState(statusBar, iconDownloads)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationSettingsUiState()
    )

    fun setStatusBarEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setStatusBarNotificationsEnabled(enabled)
    }

    fun setIconDownloadsEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setIconDownloadNotificationsEnabled(enabled)
    }
}
