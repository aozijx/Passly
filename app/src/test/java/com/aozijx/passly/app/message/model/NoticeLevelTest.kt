package com.aozijx.passly.app.message.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NoticeLevelTest {
    @Test
    fun toastDurationUsesExplicitSeverityPolicy() {
        assertEquals(false, NoticeLevel.INFO.longToast)
        assertEquals(false, NoticeLevel.SUCCESS.longToast)
        assertEquals(false, NoticeLevel.WARNING.longToast)
        assertEquals(true, NoticeLevel.ERROR.longToast)
        assertEquals(true, NoticeLevel.CRITICAL.longToast)
    }
}
