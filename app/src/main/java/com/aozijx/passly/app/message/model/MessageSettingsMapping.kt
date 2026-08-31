package com.aozijx.passly.app.message.model

import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageTopic

/** Event metadata (topic/level) → settings-domain keys. */
fun NoticeTopic.toMessageTopic(): MessageTopic = when (this) {
    NoticeTopic.CLIPBOARD -> MessageTopic.CLIPBOARD
    NoticeTopic.APP_LIFECYCLE -> MessageTopic.APP_LIFECYCLE
    NoticeTopic.BACKUP -> MessageTopic.BACKUP
    NoticeTopic.SECURITY -> MessageTopic.SECURITY
    NoticeTopic.DATABASE -> MessageTopic.DATABASE
}

fun NoticeLevel.toMessageLevel(): MessageLevel = when (this) {
    NoticeLevel.INFO -> MessageLevel.INFO
    NoticeLevel.SUCCESS -> MessageLevel.SUCCESS
    NoticeLevel.WARNING -> MessageLevel.WARNING
    NoticeLevel.ERROR -> MessageLevel.ERROR
    NoticeLevel.CRITICAL -> MessageLevel.CRITICAL
}
