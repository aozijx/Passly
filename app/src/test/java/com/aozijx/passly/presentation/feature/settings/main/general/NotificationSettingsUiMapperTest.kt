package com.aozijx.passly.presentation.feature.settings.main.general

import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import com.aozijx.passly.presentation.ui.settings.general.NotificationTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NotificationSettingsUiMapperTest {

    @Test
    fun `maps domain topic settings to stable ui topic order`() {
        val result = NotificationSettingsUiState(
            optionalMessagesEnabled = true,
            systemNotificationsEnabled = false,
            topicSettings = mapOf(
                MessageTopic.CLIPBOARD to TopicMessageSettings(enabled = false),
            ),
        ).toUiModel()

        assertFalse(result.systemNotificationsEnabled)
        assertEquals(NotificationTopic.entries, result.topics.map { it.topic })
        assertFalse(result.topics.first { it.topic == NotificationTopic.CLIPBOARD }.enabled)
    }

    @Test
    fun `ui topic maps back to domain topic`() {
        NotificationTopic.entries.forEach {
            assertEquals(MessageTopic.valueOf(it.name), it.toFeatureModel())
        }
    }
}
