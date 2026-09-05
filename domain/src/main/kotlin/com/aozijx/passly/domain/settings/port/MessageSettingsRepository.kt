package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageTopic
import kotlinx.coroutines.flow.Flow

interface MessageSettingsSource {
    val messages: Flow<MessageSettings>
}

interface MessageSettingsRepository : MessageSettingsSource {
    suspend fun setOptionalMessagesEnabled(enabled: Boolean)
    suspend fun setSystemNotificationsEnabled(enabled: Boolean)
    suspend fun setTopicEnabled(topic: MessageTopic, enabled: Boolean)
    suspend fun setTopicMinimumLevel(topic: MessageTopic, level: MessageLevel)
}
