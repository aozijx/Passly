package com.aozijx.passly.ui.features.settings.contract

import com.aozijx.passly.domain.model.SwipeActionType

data class SettingsUiState(
    val isAppPasswordEnabled: Boolean = false,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
    val isLoading: Boolean = false
)