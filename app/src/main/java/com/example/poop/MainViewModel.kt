package com.example.poop

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.core.crypto.BiometricHelper
import com.example.poop.data.local.AppDatabase
import com.example.poop.data.local.AppPrefs
import com.example.poop.data.repository.VaultRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 全局控制中心：负责生物识别认证、自动锁定逻辑以及全局 UI 设置。
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: VaultRepository by lazy {
        val database = AppDatabase.getDatabase(application)
        VaultRepository(database.vaultDao(), AppPrefs(application))
    }

    // --- 安全锁定逻辑 ---
    private val lockTimeMs = 60000L
    private var lockJob: Job? = null
    private var lastInteractionTime = System.currentTimeMillis()
    var isAuthorized by mutableStateOf(false)
        private set

    // --- 全局 UI 状态 ---
    val isDarkMode: StateFlow<Boolean?> = repository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val isDynamicColor: StateFlow<Boolean> = repository.isDynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "",
        onError: ((String) -> Unit)? = null,
        onSuccess: () -> Unit
    ) {
        BiometricHelper.authenticate(
            activity, title, subtitle,
            onSuccess = { 
                updateInteraction()
                onSuccess() 
            }, 
            onError = onError
        )
    }

    fun authorize() {
        isAuthorized = true
        updateInteraction()
    }

    fun lock() {
        isAuthorized = false
        lockJob?.cancel()
    }

    fun updateInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        lockJob?.cancel()
        lockJob = viewModelScope.launch { 
            delay(lockTimeMs)
            if (isAuthorized) lock() 
        }
    }

    fun checkAndLock() {
        if (isAuthorized && System.currentTimeMillis() - lastInteractionTime >= lockTimeMs) {
            lock()
        }
    }
}
