package com.aozijx.passly.domain.model.settings

object LockTimeoutConstraints {
    const val MIN_MS = 10_000L
    const val SLIDER_MIN_MS = 15_000L
    const val MAX_MS = 300_000L
    const val SLIDER_STEP_MS = 5_000L
}

object TabLayoutConstraints {
    const val MIN_TABS_WITHOUT_SCROLL = 2
    const val MAX_TABS_WITHOUT_SCROLL = 8
}
