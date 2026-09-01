package com.aozijx.passly.app.message.model

import java.util.UUID

data class AppNotice(
    val eventId: String,
    val code: NoticeCode,
    val arguments: Map<ArgumentKey, ArgumentValue> = emptyMap()
) {
    init {
        require(eventId.isNotBlank()) { "eventId must not be blank" }
    }
}

fun newAppNotice(
    code: NoticeCode,
    arguments: Map<ArgumentKey, ArgumentValue> = emptyMap(),
    eventId: String = UUID.randomUUID().toString()
): AppNotice = AppNotice(
    eventId = eventId,
    code = code,
    arguments = arguments
)

enum class NoticeCode {
    CLIPBOARD_CLEARED,
    CLIPBOARD_CLEAR_FAILED,
    APP_LOCKED,
    APP_CLOSE_REMINDER,
    BACKUP_EXPORT_COMPLETED,
    BACKUP_EXPORT_FAILED,
    BACKUP_IMPORT_COMPLETED,
    BACKUP_IMPORT_FAILED,
    BACKUP_DIRECTORY_CHECK_COMPLETED,
    BACKUP_DIRECTORY_CHECK_FAILED,
    SECURITY_KEY_INVALIDATED,
    SECURITY_RECOVERY_REQUIRED,
    SECURITY_ACTION_FAILED,
    DATABASE_INDEX_REBUILD_COMPLETED,
    DATABASE_INDEX_REBUILD_FAILED,
    DATABASE_OPERATION_FAILED,
    NOTIFICATION_PERMISSION_DENIED
}

enum class NoticeTopic {
    CLIPBOARD,
    APP_LIFECYCLE,
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

enum class DeliveryPolicy {
    IN_APP_ONLY,
    SYSTEM_ONLY,
    PREFER_IN_APP,
    PREFER_SYSTEM,
    BOTH
}
