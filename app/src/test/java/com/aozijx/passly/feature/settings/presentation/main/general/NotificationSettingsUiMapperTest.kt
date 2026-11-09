package com.aozijx.passly.feature.settings.presentation.main.general

import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import com.aozijx.passly.testing.sampleMessageSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSettingsUiMapperTest {
    @Test
    fun `missing topic settings use defaults in stable ui topic order`() {
        val result =
            sampleMessageSettings(
                optionalMessagesEnabled = false,
                systemNotificationsEnabled = false,
                topicSettings =
                    mapOf(
                        MessageTopic.CLIPBOARD to TopicMessageSettings(enabled = false, minimumLevel = MessageLevel.INFO),
                        MessageTopic.BACKUP to TopicMessageSettings(enabled = false, minimumLevel = MessageLevel.INFO),
                    ),
            ).toUiModel()

        assertEquals(
            NotificationSettingsUiModel(
                systemNotificationsEnabled = false,
                optionalMessagesEnabled = false,
                topics =
                    listOf(
                        NotificationTopicUiModel(MessageTopic.CLIPBOARD, enabled = false),
                        NotificationTopicUiModel(MessageTopic.APP_LIFECYCLE, enabled = true),
                        NotificationTopicUiModel(MessageTopic.BACKUP, enabled = false),
                        NotificationTopicUiModel(MessageTopic.SECURITY, enabled = true),
                        NotificationTopicUiModel(MessageTopic.DATABASE, enabled = true),
                    ),
            ),
            result,
        )
    }
}
