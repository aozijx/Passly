package com.aozijx.passly.ui.screens.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.R
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.AppPreference
import com.aozijx.passly.i8n.LanguageOption
import com.aozijx.passly.i8n.LocaleConfigReader
import com.aozijx.passly.ui.navigation.Screen
import com.aozijx.passly.util.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    val language: String = "",
    val cacheSize: String = "0.00 KB",
    val showPermissionGuide: Boolean = false,
    val logContent: String? = null,
    val isLogLoading: Boolean = false,
    val logError: String? = null,
    val logTitle: String = "日志详情",
    val exportStatus: ExportStatus? = null,
    val languages: List<LanguageOption> = emptyList(),
    val changelogUrl: String = ""
)

sealed class ExportStatus {
    object Loading : ExportStatus()
    data class Success(val intent: Intent) : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preference = AppPreference(application)
    private val permissionManager = PermissionManager.getInstance()
    private val _cacheSize = MutableStateFlow("0.00 KB")
    private val _showPermissionGuide = MutableStateFlow(false)
    private val _logContent = MutableStateFlow<String?>(null)
    private val _isLogLoading = MutableStateFlow(false)
    private val _logError = MutableStateFlow<String?>(null)
    private val _logTitle = MutableStateFlow("日志详情")
    private val _exportStatus = MutableStateFlow<ExportStatus?>(null)

    private var versionTapCount = 0

    val uiState: StateFlow<SettingsUiState> = combine(
        preference.isDarkMode,
        preference.isDynamicColor,
        preference.isNotificationsEnabled,
        preference.language,
        _cacheSize,
        _showPermissionGuide,
        _logContent,
        _isLogLoading,
        _logError,
        _logTitle,
        _exportStatus
    ) { args ->
        SettingsUiState(
            isDarkMode = args[0] as Boolean,
            isDynamicColor = args[1] as Boolean,
            isNotificationsEnabled = args[2] as Boolean,
            language = args[3] as String,
            cacheSize = args[4] as String,
            showPermissionGuide = args[5] as Boolean,
            logContent = args[6] as String?,
            isLogLoading = args[7] as Boolean,
            logError = args[8] as String?,
            logTitle = args[9] as String,
            exportStatus = args[10] as ExportStatus?,
            languages = LocaleConfigReader.getSupportedLanguages(getApplication()),
            changelogUrl = getApplication<Application>().getString(R.string.changelog)
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

    fun setLanguage(language: String) {
        viewModelScope.launch {
            preference.setLanguage(language)
            val appLocale: LocaleListCompat = if (language.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language)
            }
            AppCompatDelegate.setApplicationLocales(appLocale)
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
        _logTitle.value = "更新日志"
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
                        throw Exception("获取远程日志失败 (HTTP ${connection.responseCode})")
                    }
                }
                _logContent.value = content
            } catch (e: Exception) {
                Logcat.e("SettingsViewModel", "Log fetching failed: ${e.message}", e)
                _logError.value = "无法获取更新日志，请稍后再试。\n错误信息: ${e.message}"
            } finally {
                _isLogLoading.value = false
            }
        }
    }

    fun viewLocalLogs() {
        _logTitle.value = "本地日志"
        viewModelScope.launch {
            _isLogLoading.value = true
            _logError.value = null
            try {
                val content = withContext(Dispatchers.IO) {
                    val logFolder = Logcat.getLogFolder()
                    if (logFolder == null || !logFolder.exists()) {
                        throw Exception("日志目录不存在")
                    }

                    val logFiles = logFolder.listFiles { file -> file.extension == "log" }
                        ?.sortedByDescending { it.lastModified() }

                    if (logFiles.isNullOrEmpty()) {
                        throw NoSuchElementException("目前没有任何日志记录")
                    }

                    val sb = StringBuilder()
                    logFiles.take(5).forEach { file ->
                        sb.appendLine("=== ${file.name} ===")
                        sb.appendLine(file.readText())
                        sb.appendLine()
                    }
                    sb.toString()
                }
                _logContent.value = content
            } catch (e: NoSuchElementException) {
                _logError.value = e.message
            } catch (e: Exception) {
                _logError.value = "读取日志失败: ${e.message}"
            } finally {
                _isLogLoading.value = false
            }
        }
    }

    fun clearLogContent() {
        _logContent.value = null
        _logError.value = null
        _logTitle.value = "日志详情"
    }

    fun exportLogs() {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Loading
            try {
                val intent = withContext(Dispatchers.IO) {
                    val logFolder = Logcat.getLogFolder() ?: throw Exception("日志目录不存在")
                    val logFiles = logFolder.listFiles { file -> file.extension == "log" }

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
                _exportStatus.value = ExportStatus.Error(e.message ?: "暂无日志")
            } catch (e: Exception) {
                Logcat.e("SettingsViewModel", "Export logs failed", e)
                _exportStatus.value = ExportStatus.Error(e.message ?: "导出失败")
            }
        }
    }

    fun clearExportStatus() {
        _exportStatus.value = null
    }

    fun onVersionClick(context: Context) {
        versionTapCount++
        viewModelScope.launch {
            if (versionTapCount >= 3) {
                if (Screen.isVaultAvailable()) {
                    val intent = Intent().apply {
                        setClassName(context.packageName, BuildConfig.VAULT_ACTIVITY_CLASS)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "当前版本不支持保险箱", Toast.LENGTH_SHORT).show()
                }
                versionTapCount = 0
            }
            delay(2000)
            if (versionTapCount > 0) versionTapCount = 0
        }
    }
    
    fun openPermissionSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
