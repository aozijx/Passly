package com.aozijx.passly.domain.notice.model

data class AppNotice(
    val eventId: String,
    val topic: NoticeTopic,
    val level: NoticeLevel,
    val content: NoticeContent,
    val deliveryHint: DeliveryHint = DeliveryHint.AUTO,
    val required: Boolean = false
)

enum class NoticeTopic {
    CLIPBOARD,
    APP_LIFECYCLE,
    ICON_DOWNLOAD,
    BACKUP,
    SECURITY,
    DATABASE
}

enum class NoticeLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    CRITICAL
}

enum class DeliveryHint {
    AUTO,
    TOAST,
    STATUS_BAR,
    SILENT
}

data class NoticeContent(
    val title: String? = null,
    val text: String,
    val detail: String? = null
)
