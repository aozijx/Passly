package com.aozijx.passly.domain.settings.model

import org.junit.Assert.assertThrows
import org.junit.Test

class MessageSettingsTest {
    @Test
    fun `incomplete topic preferences are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            MessageSettings(
                optionalMessagesEnabled = true,
                systemNotificationsEnabled = true,
                topicSettings = emptyMap(),
            )
        }
    }
}
