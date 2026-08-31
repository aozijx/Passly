package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.MessagePreferences
import com.aozijx.passly.data.local.datastore.settings.NoticeLevelProto
import com.aozijx.passly.data.local.datastore.settings.TopicMessagePreference
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import com.aozijx.passly.domain.settings.model.defaultMessageTopicSettings

// -- MessageTopic --
internal fun String.toMessageTopicDomain(): MessageTopic = when (this) {
    "clipboard" -> MessageTopic.CLIPBOARD
    "app_lifecycle" -> MessageTopic.APP_LIFECYCLE
    "backup" -> MessageTopic.BACKUP
    "security" -> MessageTopic.SECURITY
    "database" -> MessageTopic.DATABASE
    else -> MessageTopic.CLIPBOARD
}

internal fun MessageTopic.toMessageTopicString(): String = when (this) {
    MessageTopic.CLIPBOARD -> "clipboard"
    MessageTopic.APP_LIFECYCLE -> "app_lifecycle"
    MessageTopic.BACKUP -> "backup"
    MessageTopic.SECURITY -> "security"
    MessageTopic.DATABASE -> "database"
}

// -- MessageLevel --
internal fun MessageLevel.toProto(): NoticeLevelProto = when (this) {
    MessageLevel.INFO -> NoticeLevelProto.NOTICE_LEVEL_INFO
    MessageLevel.SUCCESS -> NoticeLevelProto.NOTICE_LEVEL_SUCCESS
    MessageLevel.WARNING -> NoticeLevelProto.NOTICE_LEVEL_WARNING
    MessageLevel.ERROR -> NoticeLevelProto.NOTICE_LEVEL_ERROR
    MessageLevel.CRITICAL -> NoticeLevelProto.NOTICE_LEVEL_CRITICAL
}

internal fun NoticeLevelProto.toDomain(): MessageLevel = when (this) {
    NoticeLevelProto.NOTICE_LEVEL_INFO -> MessageLevel.INFO
    NoticeLevelProto.NOTICE_LEVEL_SUCCESS -> MessageLevel.SUCCESS
    NoticeLevelProto.NOTICE_LEVEL_WARNING -> MessageLevel.WARNING
    NoticeLevelProto.NOTICE_LEVEL_ERROR -> MessageLevel.ERROR
    NoticeLevelProto.NOTICE_LEVEL_CRITICAL -> MessageLevel.CRITICAL
}

// -- MessagePreferences encode / decode --
internal fun decodeMessageSettings(proto: MessagePreferences?): MessageSettings {
    if (proto == null) return MessageSettings()
    val configured = proto.topicsList.associate { item ->
        item.topicKey.toMessageTopicDomain() to TopicMessageSettings(
            enabled = item.enabled,
            minimumLevel = item.minimumLevel.toDomain()
        )
    }
    return MessageSettings(
        optionalMessagesEnabled = proto.optionalMessagesEnabled,
        systemNotificationsEnabled = proto.systemNotificationsEnabled,
        topicSettings = defaultMessageTopicSettings() + configured
    )
}

internal fun encodeMessageSettings(settings: MessageSettings): MessagePreferences =
    MessagePreferences.newBuilder()
        .setOptionalMessagesEnabled(settings.optionalMessagesEnabled)
        .setSystemNotificationsEnabled(settings.systemNotificationsEnabled)
        .addAllTopics(
            MessageTopic.entries.map { topic ->
                val value = settings.topic(topic)
                TopicMessagePreference.newBuilder()
                    .setTopicKey(topic.toMessageTopicString())
                    .setEnabled(value.enabled)
                    .setMinimumLevel(value.minimumLevel.toProto())
                    .build()
            }
        )
        .build()
