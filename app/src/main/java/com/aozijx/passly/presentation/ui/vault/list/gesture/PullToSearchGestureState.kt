package com.aozijx.passly.presentation.ui.vault.list.gesture

internal class PullToSearchGestureState(
    private val thresholdPx: Float,
    private val onProgressChanged: (Float) -> Unit,
    private val onTriggered: () -> Unit,
) {
    private var distance = 0f
    private var triggered = false

    fun onPull(deltaY: Float, isAtTop: Boolean) {
        if (!isAtTop) {
            if (!triggered) clearDistance()
            return
        }
        if (triggered || deltaY == 0f) return
        val previousDistance = distance
        distance = (distance + deltaY).coerceAtLeast(0f)
        if (distance == previousDistance) return
        onProgressChanged((distance / thresholdPx).coerceIn(0f, 1f))
        if (distance >= thresholdPx) {
            triggered = true
            onTriggered()
        }
    }

    fun reset() {
        val hadDistance = distance != 0f
        distance = 0f
        triggered = false
        if (hadDistance) onProgressChanged(0f)
    }

    private fun clearDistance() {
        if (distance == 0f) return
        distance = 0f
        onProgressChanged(0f)
    }
}
