package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import kotlinx.coroutines.flow.Flow

interface InteractionSettingsSource {
    val interaction: Flow<InteractionSettings>
}

interface InteractionSettingsRepository : InteractionSettingsSource {
    suspend fun setSwipeEnabled(enabled: Boolean)

    suspend fun setPullToSearchEnabled(enabled: Boolean)

    suspend fun setSwipeLeftAction(action: SwipeActionType)

    suspend fun setSwipeRightAction(action: SwipeActionType)

}
