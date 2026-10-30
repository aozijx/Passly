package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.app.message.contract.SinkResult
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultInAppNoticeSinkTest {
    @Test
    fun fullQueueReportsFailureAndPreservesAlreadyQueuedNotices() = runBlocking {
        val sink = DefaultInAppNoticeSink()
        val notices = (0 until 64).map { index ->
            newAppNotice(NoticeCode.CLIPBOARD_CLEARED, eventId = "notice-$index")
        }

        notices.forEach { notice ->
            assertEquals(SinkResult.Delivered, sink.deliver(notice))
        }
        assertEquals(
            SinkResult.Failed("notice.in_app_queue_full"),
            sink.deliver(newAppNotice(NoticeCode.CLIPBOARD_CLEARED, eventId = "overflow")),
        )

        assertEquals(notices, sink.notices.take(notices.size).toList())
    }
}
