package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.settings.model.InterfaceSettings
import kotlinx.coroutines.flow.Flow

interface InterfaceSettingsRepository {
    val interfaceSettings: Flow<InterfaceSettings>

    suspend fun setHideSystemBars(enabled: Boolean)
    suspend fun setTopBarCollapsible(enabled: Boolean)
    suspend fun setQuickFilterBarCollapsible(enabled: Boolean)
    suspend fun setAppCornerRadius(radiusDp: Float)
}
