package com.aozijx.passly.feature.settings.presentation.main.general

import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NotificationSettingsUiMapperTest {
    @Test
    fun `maps domain topic settings to stable ui topic order`() {
        val result =
            MessageSettings(
                optionalMessagesEnabled = true,
                systemNotificationsEnabled = false,
                topicSettings =
                    MessageTopic.entries.associateWith { topic ->
                        TopicMessageSettings(enabled = topic != MessageTopic.CLIPBOARD)
                    },
            ).toUiModel()

        assertFalse(result.systemNotificationsEnabled)
        assertEquals(true, result.optionalMessagesEnabled)
        assertEquals(MessageTopic.entries, result.topics.map { it.topic })
        assertEquals(
            MessageTopic.entries.map { it != MessageTopic.CLIPBOARD },
            result.topics.map { it.enabled },
        )
    }
}
