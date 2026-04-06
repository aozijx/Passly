package com.aozijx.passly

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.backup.EmergencyBackupExporter
import com.aozijx.passly.core.crypto.BiometricHelper
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.core.security.AutoLockScheduler
import com.aozijx.passly.data.local.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class MainUiState(
    val isAuthorized: Boolean = false,
    val isDarkMode: Boolean? = null,
    val isDynamicColor: Boolean = true,
    val databaseError: Throwable? = null
)

/**
 * 全局控制中心：负责生物识别认证、自动锁定逻辑以及全局 UI 设置。
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsUseCases = AppContainer.settingsUseCases

    // --- 安全锁定逻辑 ---
    private var lockTimeMs = 60000L
    private var lastInteractionTime = System.currentTimeMillis()
    private val autoLockScheduler = AutoLockScheduler(viewModelScope) {
        if (isAuthorized) lock()
    }
    var isAuthorized by mutableStateOf(false)
        private set

    // --- 数据库状态 ---
    var databaseError by mutableStateOf<Throwable?>(null)
        private set

    var emergencyBackupFile by mutableStateOf<File?>(null)
        private set

    // --- 全局 UI 状态 ---
    val isDarkMode: StateFlow<Boolean?> = settingsUseCases.isDarkMode.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    val isDynamicColor: StateFlow<Boolean> = settingsUseCases.isDynamicColor.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    private val authorizationState: StateFlow<Boolean> = snapshotFlow { isAuthorized }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            isAuthorized
        )

    private val dbErrorState: StateFlow<Throwable?> = snapshotFlow { databaseError }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            databaseError
        )

    val uiState: StateFlow<MainUiState> = combine(
        authorizationState, isDarkMode, isDynamicColor, dbErrorState
    ) { authorized, darkMode, dynamicColor, dbError ->
        MainUiState(
            isAuthorized = authorized,
            isDarkMode = darkMode,
            isDynamicColor = dynamicColor,
            databaseError = dbError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    init {
        checkDatabaseStatus()
        viewModelScope.launch {
            settingsUseCases.lockTimeout.collect { timeout ->
                lockTimeMs = timeout.coerceAtLeast(5000L)
                if (isAuthorized) {
                    scheduleLockTimer()
                }
            }
        }
    }

    private fun checkDatabaseStatus() {
        // 尝试预热数据库以检测迁移错误
        AppDatabase.preWarm(getApplication())
        databaseError = AppDatabase.initializationError
    }

    fun exportEmergencyBackup(context: Context) {
        viewModelScope.launch {
            EmergencyBackupExporter.exportOnFailure(context).onSuccess { file ->
                emergencyBackupFile = file
            }
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "",
        onError: ((String) -> Unit)? = null,
        onSuccess: () -> Unit
    ) {
        BiometricHelper.authenticate(
            activity, title, subtitle, onSuccess = {
                updateInteraction()
                onSuccess()
            }, onError = onError
        )
    }

    fun authorize() {
        isAuthorized = true
        updateInteraction()
    }

    fun lock() {
        isAuthorized = false
        autoLockScheduler.cancel()
    }

    fun updateInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        scheduleLockTimer()
    }

    fun checkAndLock() {
        if (isAuthorized && System.currentTimeMillis() - lastInteractionTime >= lockTimeMs) {
            lock()
        }
    }

    private fun scheduleLockTimer() {
        autoLockScheduler.schedule(lockTimeMs)
    }
}
