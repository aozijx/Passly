package com.aozijx.passly.domain.settings.model

data class InteractionSettings(
    val isSwipeEnabled: Boolean,
    val isPullToSearchEnabled: Boolean,
    val swipeLeftAction: SwipeActionType,
    val swipeRightAction: SwipeActionType,
)
