package com.aozijx.passly.domain.notice.model

data class AppMessageSettings(
    val optionalMessagesEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val topicSettings: Map<NoticeTopic, TopicMessageSettings> = defaultTopicSettings()
) {
    fun topic(topic: NoticeTopic): TopicMessageSettings =
        topicSettings[topic] ?: TopicMessageSettings()
}

data class TopicMessageSettings(
    val enabled: Boolean = true,
    val minimumLevel: NoticeLevel = NoticeLevel.INFO
)

fun defaultTopicSettings(): Map<NoticeTopic, TopicMessageSettings> = mapOf(
    NoticeTopic.CLIPBOARD to TopicMessageSettings(),
    NoticeTopic.APP_LIFECYCLE to TopicMessageSettings(),
    NoticeTopic.ICON_DOWNLOAD to TopicMessageSettings(),
    NoticeTopic.BACKUP to TopicMessageSettings(),
    NoticeTopic.SECURITY to TopicMessageSettings(minimumLevel = NoticeLevel.WARNING),
    NoticeTopic.DATABASE to TopicMessageSettings(minimumLevel = NoticeLevel.ERROR)
)
