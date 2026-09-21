package com.aozijx.passly.presentation.feature.vault.list.ui.gesture

internal class PullToSearchGestureState(
    private val thresholdPx: Float,
    private val onProgressChanged: (Float) -> Unit,
    private val onTriggered: () -> Unit,
) {
    private var distance = 0f

    fun onPull(deltaY: Float, isAtTop: Boolean) {
        if (!isAtTop) {
            clearDistance()
            return
        }
        if (deltaY == 0f) return
        val previousDistance = distance
        distance = (distance + deltaY).coerceAtLeast(0f)
        if (distance == previousDistance) return
        onProgressChanged((distance / thresholdPx).coerceIn(0f, 1f))
    }

    fun onRelease() {
        val shouldTrigger = distance >= thresholdPx
        reset()
        if (shouldTrigger) onTriggered()
    }

    fun reset() {
        val hadDistance = distance != 0f
        distance = 0f
        if (hadDistance) onProgressChanged(0f)
    }

    private fun clearDistance() {
        if (distance == 0f) return
        distance = 0f
        onProgressChanged(0f)
    }
}
