package com.aozijx.passly.data.settings.model

object LockTimeoutConstraints {
    const val MIN_MS = 10_000L
    const val SLIDER_MIN_MS = 15_000L
    const val MAX_MS = 300_000L
    const val SLIDER_STEP_MS = 5_000L
}

object InterfaceStyleConstraints {
    const val MIN_OUTER_RADIUS_DP = 0f
    const val MAX_OUTER_RADIUS_DP = 48f
    const val DEFAULT_OUTER_RADIUS_DP = 16f

    const val MIN_INNER_RADIUS_DP = 0f
    const val MAX_INNER_RADIUS_DP = 24f
    const val DEFAULT_INNER_RADIUS_DP = 6f

    const val MIN_ITEM_SPACING_DP = 0f
    const val MAX_ITEM_SPACING_DP = 12f
    const val DEFAULT_ITEM_SPACING_DP = 2f

    const val MIN_CONTENT_PADDING_DP = 8f
    const val MAX_CONTENT_PADDING_DP = 32f
    const val DEFAULT_CONTENT_PADDING_DP = 16f
}
