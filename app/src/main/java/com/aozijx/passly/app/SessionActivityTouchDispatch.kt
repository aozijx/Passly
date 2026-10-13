package com.aozijx.passly.app

import android.view.MotionEvent
import android.view.Window
import com.aozijx.passly.domain.access.port.SessionActivityReporter

internal class SessionActivityTouchDispatch(
    private val sessionActivityReporter: SessionActivityReporter,
) {
    fun deliver(actionMasked: Int, deliverToContent: () -> Boolean): Boolean {
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            sessionActivityReporter.onUserInteraction()
        }
        return deliverToContent()
    }
}

internal class SessionActivityWindowCallback(
    private val delegate: Window.Callback,
    sessionActivityReporter: SessionActivityReporter,
) : Window.Callback by delegate {
    private val touchDispatch = SessionActivityTouchDispatch(sessionActivityReporter)

    override fun dispatchTouchEvent(event: MotionEvent): Boolean =
        touchDispatch.deliver(event.actionMasked) {
            delegate.dispatchTouchEvent(event)
        }
}
