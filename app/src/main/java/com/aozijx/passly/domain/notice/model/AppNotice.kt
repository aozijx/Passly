package com.aozijx.passly.domain.notice.model

data class AppNotice(
    val eventId: String,
    val code: NoticeCode,
    val topic: NoticeTopic,
    val level: NoticeLevel,
    val arguments: Map<ArgumentKey, ArgumentValue>,
    val deliveryPolicy: DeliveryPolicy
)

enum class NoticeCode {
    CLIPBOARD_CLEARED,
    CLIPBOARD_CLEAR_FAILED,
    APP_LOCKED,
    APP_CLOSE_REMINDER,
    ICON_DOWNLOAD_COMPLETED,
    ICON_DOWNLOAD_FAILED,
    BACKUP_EXPORT_COMPLETED,
    BACKUP_EXPORT_FAILED,
    BACKUP_IMPORT_COMPLETED,
    BACKUP_IMPORT_FAILED,
    SECURITY_KEY_INVALIDATED,
    SECURITY_RECOVERY_REQUIRED,
    SECURITY_ACTION_FAILED,
    DATABASE_INDEX_REBUILD_COMPLETED,
    DATABASE_INDEX_REBUILD_FAILED,
    DATABASE_OPERATION_FAILED
}

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

data class DeliveryPolicy(
    /** 是否要求系统通知（状态栏） */
    val systemNotificationRequired: Boolean = false,
    /** 语义去重窗口，0 = 不做语义合并 */
    val suppressWithinMs: Long = 0,
    /** 同类聚合窗口，0 = 不聚合（与 suppressWithinMs 互斥） */
    val aggregateWithinMs: Long = 0
)
