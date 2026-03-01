package com.example.poop.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.data.Preference
import com.example.poop.util.PermissionManager
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
    val isNotificationsEnabled: Boolean = false,
    val isDarkMode: Boolean = false,
    val isDynamicColor: Boolean = true,
    val cacheSize: String = "0.00 KB",
    val showPermissionGuide: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preference = Preference(application)
    private val permissionManager = PermissionManager.getInstance()
    private val _cacheSize = MutableStateFlow("0.00 KB")
    private val _showPermissionGuide = MutableStateFlow(false)

    // 将偏好设置类的 Flow 组合在一起
    private val preferenceFlow = combine(
        preference.isDarkMode,
        preference.isDynamicColor,
        preference.isNotificationsEnabled
    ) { dark, dynamic, notify ->
        Triple(dark, dynamic, notify)
    }

    // 将状态类的 Flow 组合在一起
    private val statusFlow = combine(
        _cacheSize,
        _showPermissionGuide
    ) { cache, guide ->
        Pair(cache, guide)
    }

    // 最终组合两个分组 Flow，解决类型推断问题并提高可读性
    val uiState: StateFlow<SettingsUiState> = combine(
        preferenceFlow,
        statusFlow
    ) { pref, status ->
        SettingsUiState(
            isDarkMode = pref.first,
            isDynamicColor = pref.second,
            isNotificationsEnabled = pref.third,
            cacheSize = status.first,
            showPermissionGuide = status.second
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
        if (isEnabled) {
            if (permissionManager.hasNotificationPermission(getApplication())) {
                viewModelScope.launch { preference.setNotificationsEnabled(true) }
            } else {
                permissionManager.requestNotificationPermission { result ->
                    when (result) {
                        PermissionManager.PermissionResult.Granted -> {
                            viewModelScope.launch { preference.setNotificationsEnabled(true) }
                        }
                        PermissionManager.PermissionResult.PermanentlyDenied -> {
                            _showPermissionGuide.value = true
                        }
                        else -> {
                            viewModelScope.launch { preference.setNotificationsEnabled(false) }
                        }
                    }
                }
            }
        } else {
            viewModelScope.launch { preference.setNotificationsEnabled(false) }
        }
    }
    
    fun dismissPermissionGuide() {
        _showPermissionGuide.value = false
    }

    fun toggleDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            preference.setDarkMode(isDarkMode)
        }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preference.setDynamicColor(enabled)
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
