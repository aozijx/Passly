package com.example.poop.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.util.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

data class SettingsUiState(
    val isNotificationsEnabled: Boolean = true,
    val isDarkMode: Boolean = false,
    val cacheSize: String = "0.00 KB"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preference = Preference(application)
    
    private val _isNotificationsEnabled = MutableStateFlow(true)
    private val _cacheSize = MutableStateFlow("0.00 KB")

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

    init {
        updateCacheSize()
    }

    private fun updateCacheSize() {
        viewModelScope.launch {
            val size = getCacheSize()
            _cacheSize.value = formatFileSize(size)
        }
    }

    private suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        val cacheDir = getApplication<Application>().cacheDir
        getFolderSize(cacheDir)
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0
        var size: Long = 0
        file.listFiles()?.forEach {
            size += if (it.isDirectory) getFolderSize(it) else it.length()
        }
        return size
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0.00 KB"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return String.format(Locale.US, "%.2f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }

    fun toggleNotifications(isEnabled: Boolean) {
        _isNotificationsEnabled.value = isEnabled
    }

    fun toggleDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            preference.setDarkMode(isDarkMode)
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = getApplication<Application>().cacheDir
            deleteDir(cacheDir)
            updateCacheSize()
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            children?.forEach {
                val success = deleteDir(File(dir, it))
                if (!success) return false
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        }
        return false
    }
}
