package com.aozijx.passly.app

import android.view.MotionEvent
import com.aozijx.passly.domain.access.port.SessionActivityReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionActivityTouchDispatchTest {
    @Test
    fun `down reports activity before content handles the touch`() {
        val events = mutableListOf<String>()
        val dispatch = SessionActivityTouchDispatch(
            sessionActivityReporter = recordingReporter { events += "activity" },
        )

        val handled = dispatch.deliver(MotionEvent.ACTION_DOWN) {
            events += "content"
            true
        }

        assertTrue(handled)
        assertEquals(listOf("activity", "content"), events)
    }

    @Test
    fun `non-down touch is delegated without reporting activity`() {
        var activityReports = 0
        var contentDeliveries = 0
        val dispatch = SessionActivityTouchDispatch(
            sessionActivityReporter = recordingReporter { activityReports++ },
        )

        val handled = dispatch.deliver(MotionEvent.ACTION_MOVE) {
            contentDeliveries++
            false
        }

        assertFalse(handled)
        assertEquals(0, activityReports)
        assertEquals(1, contentDeliveries)
    }

    private fun recordingReporter(onReport: () -> Unit) = object : SessionActivityReporter {
        override fun onUserInteraction() = onReport()
    }
}
