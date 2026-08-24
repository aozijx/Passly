package com.aozijx.passly.presentation.feature.settings.main

import com.aozijx.passly.domain.settings.model.SwipeActionType

data class SettingsUiState(
    val isAppPasswordEnabled: Boolean = false,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
    val isLoading: Boolean = false,
    val isClearingDatabase: Boolean = false
)
