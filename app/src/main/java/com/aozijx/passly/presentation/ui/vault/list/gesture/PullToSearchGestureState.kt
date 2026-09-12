package com.aozijx.passly.presentation.ui.vault.list.gesture

internal class PullToSearchGestureState(
    private val thresholdPx: Float,
    private val onTriggered: () -> Unit,
) {
    private var distance = 0f
    private var triggered = false

    fun onPull(deltaY: Float, isAtTop: Boolean) {
        if (!isAtTop || deltaY <= 0f) {
            if (!triggered) distance = 0f
            return
        }
        if (triggered) return
        distance += deltaY
        if (distance >= thresholdPx) {
            triggered = true
            onTriggered()
        }
    }

    fun reset() {
        distance = 0f
        triggered = false
    }
}
