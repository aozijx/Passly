package com.aozijx.passly.presentation.feature.settings.main.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.cache.AppCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 通用设置页的页面级 ViewModel（MVI）：
 * 持有缓存状态/动作（原 CacheSettingsViewModel）与页面级导航效果。
 */
@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val appCacheManager: AppCacheManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneralSettingsUiState())
    val uiState: StateFlow<GeneralSettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<GeneralSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** 缓存操作互斥：新操作会取消仍在进行的旧操作，避免并发读写缓存目录。 */
    private var cacheOperationJob: Job? = null

    init {
        onAction(GeneralSettingsAction.RefreshCache)
    }

    fun onAction(action: GeneralSettingsAction) {
        when (action) {
            GeneralSettingsAction.RefreshCache -> refreshCacheSize()
            GeneralSettingsAction.ClearCache -> clearCache()
        }
    }

    fun openAppDetails() {
        _effects.trySend(GeneralSettingsEffect.OpenAppDetails)
    }

    fun openTerms() {
        _effects.trySend(GeneralSettingsEffect.OpenTerms)
    }

    fun openPrivacyPolicy() {
        _effects.trySend(GeneralSettingsEffect.OpenPrivacyPolicy)
    }

    fun openOpenSourceLicenses() {
        _effects.trySend(GeneralSettingsEffect.OpenOpenSourceLicenses)
    }

    private fun refreshCacheSize() {
        cacheOperationJob?.cancel()
        cacheOperationJob = viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true) }
            val size = withContext(Dispatchers.IO) {
                appCacheManager.calculateTotalSize()
            }
            _uiState.update { it.copy(cacheSize = size, isCalculating = false) }
        }
    }

    private fun clearCache() {
        cacheOperationJob?.cancel()
        cacheOperationJob = viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true) }
            val size = withContext(Dispatchers.IO) {
                appCacheManager.clearAll()
                appCacheManager.calculateTotalSize()
            }
            _uiState.update { it.copy(cacheSize = size, isCalculating = false) }
            _effects.trySend(GeneralSettingsEffect.CacheCleared)
        }
    }
}
