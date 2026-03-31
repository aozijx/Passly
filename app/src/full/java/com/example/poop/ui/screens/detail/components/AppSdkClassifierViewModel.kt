package com.example.poop.ui.screens.detail.components

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.core.util.AppWithSdk
import com.example.poop.core.util.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// UI State for the screen
data class AppSdkClassifierUiState(
    val sdkAppMap: Map<Int, List<AppWithSdk>> = emptyMap(),
    val isLoading: Boolean = false,
    val loadStatus: String = "等待扫描...",
    val expandedSdks: Set<Int> = emptySet(),
    val hasPermission: Boolean = true,
    val includeSystemApps: Boolean = false
)

class AppSdkClassifierViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppSdkClassifierUiState())
    val uiState: StateFlow<AppSdkClassifierUiState> = _uiState.asStateFlow()

    fun startScan(context: Context) {
        if (_uiState.value.isLoading) return

        val includeSystem = _uiState.value.includeSystemApps
        _uiState.update { it.copy(isLoading = true, loadStatus = "正在深度扫描架构信息...") }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                PackageUtils.getAllInstalledApps(context, includeSystem)
            }

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

    fun toggleSystemApps(context: Context) {
        _uiState.update { it.copy(includeSystemApps = !it.includeSystemApps) }
        startScan(context) // 切换后自动重新扫描
    }
}
