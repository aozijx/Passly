package com.aozijx.passly.presentation.feature.settings.main.general

import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NotificationSettingsUiMapperTest {

    @Test
    fun `maps domain topic settings to stable ui topic order`() {
        val result = NotificationSettingsUiState.Ready(
            optionalMessagesEnabled = true,
            systemNotificationsEnabled = false,
            runtimeNotificationPermissionGranted = true,
            notificationsEnabledBySystem = true,
            notificationChannelEnabled = true,
            topicSettings = MessageTopic.entries.associateWith { topic ->
                TopicMessageSettings(enabled = topic != MessageTopic.CLIPBOARD)
            },
        ).toUiModel()

        assertFalse(result.systemNotificationsEnabled)
        assertEquals(MessageTopic.entries, result.topics.map { it.topic })
        assertFalse(result.topics.first { it.topic == MessageTopic.CLIPBOARD }.enabled)
    }
}
