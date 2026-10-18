package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.MessageLevelPreference
import com.aozijx.passly.data.preferences.proto.MessagePreferences
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings

internal fun MessageLevel.toProto(): MessageLevelPreference = when (this) {
    MessageLevel.INFO -> MessageLevelPreference.MESSAGE_LEVEL_INFO
    MessageLevel.SUCCESS -> MessageLevelPreference.MESSAGE_LEVEL_SUCCESS
    MessageLevel.WARNING -> MessageLevelPreference.MESSAGE_LEVEL_WARNING
    MessageLevel.ERROR -> MessageLevelPreference.MESSAGE_LEVEL_ERROR
    MessageLevel.CRITICAL -> MessageLevelPreference.MESSAGE_LEVEL_CRITICAL
}

internal fun MessageLevelPreference.toDomain(): MessageLevel = when (this) {
    MessageLevelPreference.MESSAGE_LEVEL_INFO -> MessageLevel.INFO
    MessageLevelPreference.MESSAGE_LEVEL_SUCCESS -> MessageLevel.SUCCESS
    MessageLevelPreference.MESSAGE_LEVEL_WARNING -> MessageLevel.WARNING
    MessageLevelPreference.MESSAGE_LEVEL_ERROR -> MessageLevel.ERROR
    MessageLevelPreference.MESSAGE_LEVEL_CRITICAL -> MessageLevel.CRITICAL
    MessageLevelPreference.MESSAGE_LEVEL_UNSPECIFIED ->
        throw IllegalArgumentException("Unsupported message level: $this")
}

internal fun decodeMessageSettings(proto: MessagePreferences): MessageSettings =
    MessageSettings(
        optionalMessagesEnabled = proto.optionalMessagesEnabled,
        systemNotificationsEnabled = proto.systemNotificationsEnabled,
        topicSettings = mapOf(
            MessageTopic.CLIPBOARD to TopicMessageSettings(
                enabled = proto.clipboardEnabled,
                minimumLevel = proto.clipboardMinimumLevel.toDomain(),
            ),
            MessageTopic.APP_LIFECYCLE to TopicMessageSettings(
                enabled = proto.appLifecycleEnabled,
                minimumLevel = proto.appLifecycleMinimumLevel.toDomain(),
            ),
            MessageTopic.BACKUP to TopicMessageSettings(
                enabled = proto.backupEnabled,
                minimumLevel = proto.backupMinimumLevel.toDomain(),
            ),
            MessageTopic.SECURITY to TopicMessageSettings(
                enabled = proto.securityEnabled,
                minimumLevel = proto.securityMinimumLevel.toDomain(),
            ),
            MessageTopic.DATABASE to TopicMessageSettings(
                enabled = proto.databaseEnabled,
                minimumLevel = proto.databaseMinimumLevel.toDomain(),
            ),
        ),
    )

internal fun encodeMessageSettings(settings: MessageSettings): MessagePreferences =
    MessagePreferences.newBuilder()
        .setOptionalMessagesEnabled(settings.optionalMessagesEnabled)
        .setSystemNotificationsEnabled(settings.systemNotificationsEnabled)
        .setClipboardEnabled(settings.topic(MessageTopic.CLIPBOARD).enabled)
        .setClipboardMinimumLevel(settings.topic(MessageTopic.CLIPBOARD).minimumLevel.toProto())
        .setAppLifecycleEnabled(settings.topic(MessageTopic.APP_LIFECYCLE).enabled)
        .setAppLifecycleMinimumLevel(
            settings.topic(MessageTopic.APP_LIFECYCLE).minimumLevel.toProto(),
        )
        .setBackupEnabled(settings.topic(MessageTopic.BACKUP).enabled)
        .setBackupMinimumLevel(settings.topic(MessageTopic.BACKUP).minimumLevel.toProto())
        .setSecurityEnabled(settings.topic(MessageTopic.SECURITY).enabled)
        .setSecurityMinimumLevel(settings.topic(MessageTopic.SECURITY).minimumLevel.toProto())
        .setDatabaseEnabled(settings.topic(MessageTopic.DATABASE).enabled)
        .setDatabaseMinimumLevel(settings.topic(MessageTopic.DATABASE).minimumLevel.toProto())
        .build()
