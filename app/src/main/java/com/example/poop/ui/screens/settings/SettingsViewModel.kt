package com.example.poop.ui.screens.settings

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.R
import com.example.poop.data.Preference
import com.example.poop.util.Logcat
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.log10
import kotlin.math.pow

data class SettingsUiState(
    val isNotificationsEnabled: Boolean = false,
    val isDarkMode: Boolean = false,
    val isDynamicColor: Boolean = true,
    val cacheSize: String = "0.00 KB",
    val showPermissionGuide: Boolean = false,
    val logContent: String? = null,
    val isLogLoading: Boolean = false,
    val logError: String? = null,
    val exportStatus: ExportStatus? = null
)

sealed class ExportStatus {
    object Loading : ExportStatus()
    data class Success(val intent: Intent) : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preference = Preference(application)
    private val permissionManager = PermissionManager.getInstance()
    private val _cacheSize = MutableStateFlow("0.00 KB")
    private val _showPermissionGuide = MutableStateFlow(false)
    private val _logContent = MutableStateFlow<String?>(null)
    private val _isLogLoading = MutableStateFlow(false)
    private val _logError = MutableStateFlow<String?>(null)
    private val _exportStatus = MutableStateFlow<ExportStatus?>(null)

    // 修复：手动将多个 Flow 组合成单一的 uiState，避免嵌套导致 Kotlin 编译器类型推断失败
    val uiState: StateFlow<SettingsUiState> = combine(
        preference.isDarkMode,
        preference.isDynamicColor,
        preference.isNotificationsEnabled,
        _cacheSize,
        _showPermissionGuide,
        _logContent,
        _isLogLoading,
        _logError,
        _exportStatus
    ) { args ->
        SettingsUiState(
            isDarkMode = args[0] as Boolean,
            isDynamicColor = args[1] as Boolean,
            isNotificationsEnabled = args[2] as Boolean,
            cacheSize = args[3] as String,
            showPermissionGuide = args[4] as Boolean,
            logContent = args[5] as String?,
            isLogLoading = args[6] as Boolean,
            logError = args[7] as String?,
            exportStatus = args[8] as ExportStatus?
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

    fun fetchLog(urlStr: String) {
        if (urlStr.isBlank() || !urlStr.startsWith("http")) {
            Logcat.e("SettingsViewModel", "Invalid URL: $urlStr. Showing fallback log.")
            _logContent.value = getApplication<Application>().getString(R.string.changelog_last)
            return
        }

        viewModelScope.launch {
            _isLogLoading.value = true
            _logError.value = null
            try {
                val content = withContext(Dispatchers.IO) {
                    val url = URL(urlStr)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.requestMethod = "GET"
                    
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        throw Exception("Server error: ${connection.responseCode}")
                    }
                }
                _logContent.value = content
            } catch (e: Exception) {
                Logcat.e("SettingsViewModel", "Log fetching failed, showing local fallback. Detail: ${e.message}", e)
                _logContent.value = getApplication<Application>().getString(R.string.changelog_last)
            } finally {
                _isLogLoading.value = false
            }
        }
    }

    fun clearLogContent() {
        _logContent.value = null
        _logError.value = null
    }

    fun exportLogs() {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Loading
            try {
                val intent = withContext(Dispatchers.IO) {
                    val logFolder = Logcat.getLogFolder() ?: throw Exception("日志目录不存在")
                    val logFiles = logFolder.listFiles { file -> file.extension == "log" }
                    
                    // 优化点：如果是空文件夹，抛出特定异常，不触发 Logcat.e 写入
                    if (logFiles.isNullOrEmpty()) {
                        throw NoSuchElementException("目前没有任何日志记录")
                    }

                    val zipFile = File(getApplication<Application>().cacheDir, "app_logs.zip")
                    ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                        logFiles.forEach { file ->
                            FileInputStream(file).use { input ->
                                val entry = ZipEntry(file.name)
                                zipOut.putNextEntry(entry)
                                input.copyTo(zipOut)
                                zipOut.closeEntry()
                            }
                        }
                    }

                    val uri = FileProvider.getUriForFile(
                        getApplication(),
                        "${getApplication<Application>().packageName}.fileprovider",
                        zipFile
                    )

                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                _exportStatus.value = ExportStatus.Success(intent)
            } catch (e: NoSuchElementException) {
                // 如果只是没文件，不要用 Logcat.e 记录（避免制造新日志），直接给 UI 报错状态
                _exportStatus.value = ExportStatus.Error(e.message ?: "暂无日志")
            } catch (e: Exception) {
                // 真正的技术故障才记录日志
                Logcat.e("SettingsViewModel", "Export logs failed", e)
                _exportStatus.value = ExportStatus.Error(e.message ?: "导出失败")
            }
        }
    }

    fun clearExportStatus() {
        _exportStatus.value = null
    }
}
