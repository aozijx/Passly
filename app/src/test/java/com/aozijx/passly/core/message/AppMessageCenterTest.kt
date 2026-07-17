package com.aozijx.passly.core.message

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppMessageCenterTest {
    @Test
    fun consecutiveDuplicateIsPublishedOnce() = runBlocking {
        val received = mutableListOf<AppMessage>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            AppMessageCenter.messages.take(2).toList(received)
        }

        val duplicateText = "duplicate-${System.nanoTime()}"
        AppMessageCenter.publish(duplicateText)
        AppMessageCenter.publish(duplicateText)
        AppMessageCenter.publish("sentinel-${System.nanoTime()}")
        collector.join()

        assertEquals(2, received.size)
        assertEquals(duplicateText, received.first().text)
    }
}
