package com.aozijx.passly.domain.settings.model

/**
 * Message-center preferences owned by the settings domain.
 *
 * The notification *event* model (`AppNotice`) lives in the app layer
 * (`app.message.model`); this type is the settings value that the data layer
 * serializes to proto and the app notification router consumes.
 */
data class MessageSettings(
    val optionalMessagesEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val topicSettings: Map<MessageTopic, TopicMessageSettings> = defaultMessageTopicSettings()
) {
    fun topic(topic: MessageTopic): TopicMessageSettings =
        topicSettings[topic] ?: TopicMessageSettings()
}

data class TopicMessageSettings(
    val enabled: Boolean = true,
    val minimumLevel: MessageLevel = MessageLevel.INFO
)

enum class MessageTopic {
    CLIPBOARD,
    APP_LIFECYCLE,
    ICON_DOWNLOAD,
    BACKUP,
    SECURITY,
    DATABASE
}

enum class MessageLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    CRITICAL
}

fun defaultMessageTopicSettings(): Map<MessageTopic, TopicMessageSettings> = mapOf(
    MessageTopic.CLIPBOARD to TopicMessageSettings(),
    MessageTopic.APP_LIFECYCLE to TopicMessageSettings(),
    MessageTopic.ICON_DOWNLOAD to TopicMessageSettings(),
    MessageTopic.BACKUP to TopicMessageSettings(),
    MessageTopic.SECURITY to TopicMessageSettings(minimumLevel = MessageLevel.WARNING),
    MessageTopic.DATABASE to TopicMessageSettings(minimumLevel = MessageLevel.ERROR)
)
