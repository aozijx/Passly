package com.aozijx.passly.feature.settings.general

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.platform.CacheUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneralSettingsUiState())
    val uiState: StateFlow<GeneralSettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<GeneralSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

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
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true) }
            val size = withContext(Dispatchers.IO) {
                CacheUtils.calculateTotalCacheSize(context)
            }
            _uiState.update { it.copy(cacheSize = size, isCalculating = false) }
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true) }
            val size = withContext(Dispatchers.IO) {
                CacheUtils.clearAllCache(context)
                CacheUtils.calculateTotalCacheSize(context)
            }
            _uiState.update { it.copy(cacheSize = size, isCalculating = false) }
            _effects.trySend(GeneralSettingsEffect.CacheCleared)
        }
    }
}
