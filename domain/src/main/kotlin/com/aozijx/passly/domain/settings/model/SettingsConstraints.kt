package com.aozijx.passly.domain.settings.model

object LockTimeoutConstraints {
    const val MIN_MS = 10_000L
    const val SLIDER_MIN_MS = 15_000L
    const val MAX_MS = 300_000L
    const val SLIDER_STEP_MS = 5_000L
}

object AppCornerRadiusConstraints {
    const val MIN_DP = 0f
    const val MAX_DP = 48f
    const val DEFAULT_DP = 16f

    fun normalize(radiusDp: Float): Float = radiusDp.coerceIn(MIN_DP, MAX_DP)
}
