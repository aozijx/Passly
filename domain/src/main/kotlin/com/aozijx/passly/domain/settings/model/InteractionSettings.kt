package com.aozijx.passly.domain.settings.model

data class InteractionSettings(
    val isSwipeEnabled: Boolean = false,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
    val autofill: AutofillSettings = AutofillSettings(),
)
