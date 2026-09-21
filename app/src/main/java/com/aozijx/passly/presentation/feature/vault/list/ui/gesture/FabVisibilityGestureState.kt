package com.aozijx.passly.presentation.feature.vault.list.ui.gesture

internal class FabVisibilityGestureState(
    private val onVisibilityChanged: (Boolean) -> Unit,
) {
    private var isVisible = true

    fun onScroll(deltaY: Float) {
        val nextVisibility = when {
            deltaY < -SCROLL_THRESHOLD -> false
            deltaY > SCROLL_THRESHOLD -> true
            else -> return
        }
        if (nextVisibility == isVisible) return

        isVisible = nextVisibility
        onVisibilityChanged(nextVisibility)
    }

    private companion object {
        const val SCROLL_THRESHOLD = 1f
    }
}
