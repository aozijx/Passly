package com.example.poop.ui.screens.detail.component

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.util.Logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// Data model for an app
data class AppWithSdk(
    val packageName: String,
    val appName: String,
    val targetSdk: Int,
    val versionName: String,
    val architecture: String
)

// UI State for the screen
data class AppSdkClassifierUiState(
    val sdkAppMap: Map<Int, List<AppWithSdk>> = emptyMap(),
    val isLoading: Boolean = false,
    val loadStatus: String = "扫描已安装应用的架构与 SDK",
    val expandedSdks: Set<Int> = emptySet() // Manages collapsed state for each SDK version
)

class AppSdkClassifierViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppSdkClassifierUiState())
    val uiState: StateFlow<AppSdkClassifierUiState> = _uiState.asStateFlow()

    fun startScan(context: Context) {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, loadStatus = "正在深度扫描架构信息...") }

        viewModelScope.launch {
            val result = getAllAppsWithSdk(context)
            val newMap = withContext(Dispatchers.Default) {
                result.groupBy { it.targetSdk }.toSortedMap(compareByDescending { it })
            }
            _uiState.update {
                it.copy(
                    sdkAppMap = newMap,
                    isLoading = false,
                    loadStatus = "扫描完成: 共 ${result.size} 个应用"
                )
            }
        }
    }

    fun toggleSdkExpansion(sdkVersion: Int) {
        _uiState.update { state ->
            val currentExpanded = state.expandedSdks
            val newExpanded = if (sdkVersion in currentExpanded) {
                currentExpanded - sdkVersion
            } else {
                currentExpanded + sdkVersion
            }
            state.copy(expandedSdks = newExpanded)
        }
    }

    private suspend fun getAllAppsWithSdk(context: Context): List<AppWithSdk> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)

            packages.mapNotNull { pkg ->
                val appInfo = pkg.applicationInfo
                if ((appInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) == 0) {
                    val arch = try {
                        val primaryCpuAbi = ApplicationInfo::class.java.getField("primaryCpuAbi")
                            .get(appInfo) as? String
                        when {
                            primaryCpuAbi != null -> {
                                when {
                                    primaryCpuAbi.contains("arm64-v8a") -> "arm64-v8a"
                                    primaryCpuAbi.contains("armeabi-v7a") -> "armeabi-v7a"
                                    primaryCpuAbi.contains("x86_64") -> "x86_64"
                                    primaryCpuAbi.contains("x86") -> "x86"
                                    primaryCpuAbi.contains("64") -> "64-bit"
                                    else -> primaryCpuAbi
                                }
                            }
                            appInfo.nativeLibraryDir.contains("arm64") -> "arm64-v8a"
                            appInfo.nativeLibraryDir.contains("arm") -> "armeabi-v7a"
                            else -> "32-bit"
                        }
                    } catch (e: Exception) {
                        Logcat.e("AppSdkClassifier", "识别 ABI 失败: ${pkg.packageName}", e)
                        "Unknown"
                    }

                    AppWithSdk(
                        packageName = pkg.packageName,
                        appName = appInfo.loadLabel(pm).toString(),
                        targetSdk = appInfo.targetSdkVersion,
                        versionName = pkg.versionName ?: "N/A",
                        architecture = arch
                    )
                } else null
            }
        }
}
