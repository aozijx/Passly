package com.aozijx.passly.feature.settings.general

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.platform.CacheUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 缓存大小展示与清理（IO 逻辑收敛到 ViewModel，UI 只收集状态）。 */
@HiltViewModel
class CacheSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _cacheSize = MutableStateFlow<String?>(null)
    val cacheSize: StateFlow<String?> = _cacheSize.asStateFlow()

    private val _isCalculating = MutableStateFlow(true)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _isCalculating.value = true
            _cacheSize.value = withContext(Dispatchers.IO) {
                CacheUtils.calculateTotalCacheSize(context)
            }
            _isCalculating.value = false
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _isCalculating.value = true
            _cacheSize.value = withContext(Dispatchers.IO) {
                CacheUtils.clearAllCache(context)
                CacheUtils.calculateTotalCacheSize(context)
            }
            _isCalculating.value = false
        }
    }
}
