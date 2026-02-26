package com.example.poop.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.util.Preference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isNotificationsEnabled: Boolean = true,
    val isDarkMode: Boolean = false,
    val cacheSize: String = "256 MB"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preference = Preference(application)
    
    private val _isNotificationsEnabled = MutableStateFlow(true)
    private val _cacheSize = MutableStateFlow("256 MB")

    // 将 DataStore 的持久化流与 ViewModel 的内存流合并
    val uiState: StateFlow<SettingsUiState> = combine(
        preference.isDarkMode,
        _isNotificationsEnabled,
        _cacheSize
    ) { dark, notify, cache ->
        SettingsUiState(
            isDarkMode = dark,
            isNotificationsEnabled = notify,
            cacheSize = cache
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun toggleNotifications(isEnabled: Boolean) {
        _isNotificationsEnabled.value = isEnabled
    }

    fun toggleDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            preference.setDarkMode(isDarkMode)
        }
    }

    fun clearCache() {
        _cacheSize.value = "0 MB"
    }
}
