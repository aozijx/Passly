package com.aozijx.passly.domain.settings.model

/**
 * Message-center preferences owned by the settings domain.
 *
 * The notification *event* model (`AppNotice`) lives in the app layer
 * (`app.message.model`); this type is the settings value that the data layer
 * serializes to proto and the app notification router consumes.
 */
data class MessageSettings(
    val optionalMessagesEnabled: Boolean,
    val systemNotificationsEnabled: Boolean,
    val topicSettings: Map<MessageTopic, TopicMessageSettings>,
) {
    init {
        require(topicSettings.keys == MessageTopic.entries.toSet()) {
            "Message settings must define every topic exactly once"
        }
    }

    fun topic(topic: MessageTopic): TopicMessageSettings = topicSettings.getValue(topic)
}

data class TopicMessageSettings(
    val enabled: Boolean,
    val minimumLevel: MessageLevel,
)

enum class MessageTopic {
    CLIPBOARD,
    APP_LIFECYCLE,
    BACKUP,
    SECURITY,
    DATABASE,
}

enum class MessageLevel(
    val severity: Int,
) {
    INFO(10),
    SUCCESS(20),
    WARNING(30),
    ERROR(40),
    CRITICAL(50),
    ;

    fun meetsMinimum(minimum: MessageLevel): Boolean = severity >= minimum.severity
}
