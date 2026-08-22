package com.aozijx.passly.app.message.contract

import com.aozijx.passly.app.message.model.DeliveryPolicy
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.NoticeLevel
import com.aozijx.passly.app.message.model.NoticeTopic

/**
 * NoticeCode 的可信策略注册表。
 * 每个 [NoticeCode] 关联一个 [NoticeCodePolicy]，发布时由 [NoticeRouter] 查询。
 */
data class NoticeCodePolicy(
    val topic: NoticeTopic,
    val level: NoticeLevel,
    val deliveryPolicy: DeliveryPolicy,
    val mandatory: Boolean = false,
    val eventIdTtlMs: Long = 2 * 60 * 1000L,
    val suppressWithinMs: Long = 0L
)

fun interface NoticeCodeRegistry {
    fun policyFor(code: NoticeCode): NoticeCodePolicy
}

fun defaultNoticeCodePolicy(code: NoticeCode): NoticeCodePolicy = when (code) {
    NoticeCode.CLIPBOARD_CLEARED -> NoticeCodePolicy(
        NoticeTopic.CLIPBOARD,
        NoticeLevel.INFO,
        DeliveryPolicy.IN_APP_ONLY,
        suppressWithinMs = 5_000
    )

    NoticeCode.CLIPBOARD_CLEAR_FAILED -> NoticeCodePolicy(
        NoticeTopic.CLIPBOARD,
        NoticeLevel.ERROR,
        DeliveryPolicy.IN_APP_ONLY,
        suppressWithinMs = 5_000
    )

    NoticeCode.APP_LOCKED -> NoticeCodePolicy(
        NoticeTopic.APP_LIFECYCLE,
        NoticeLevel.INFO,
        DeliveryPolicy.IN_APP_ONLY,
        suppressWithinMs = 10_000
    )

    NoticeCode.APP_CLOSE_REMINDER -> NoticeCodePolicy(
        NoticeTopic.APP_LIFECYCLE,
        NoticeLevel.WARNING,
        DeliveryPolicy.IN_APP_ONLY,
        suppressWithinMs = 10_000
    )

    NoticeCode.ICON_DOWNLOAD_COMPLETED -> NoticeCodePolicy(
        NoticeTopic.ICON_DOWNLOAD,
        NoticeLevel.SUCCESS,
        DeliveryPolicy.PREFER_SYSTEM,
        suppressWithinMs = 5_000
    )

    NoticeCode.ICON_DOWNLOAD_FAILED -> NoticeCodePolicy(
        NoticeTopic.ICON_DOWNLOAD,
        NoticeLevel.ERROR,
        DeliveryPolicy.PREFER_SYSTEM,
        suppressWithinMs = 10_000
    )

    NoticeCode.BACKUP_EXPORT_COMPLETED -> NoticeCodePolicy(
        NoticeTopic.BACKUP,
        NoticeLevel.SUCCESS,
        DeliveryPolicy.PREFER_IN_APP,
        suppressWithinMs = 30_000
    )

    NoticeCode.BACKUP_EXPORT_FAILED -> NoticeCodePolicy(
        NoticeTopic.BACKUP,
        NoticeLevel.ERROR,
        DeliveryPolicy.PREFER_IN_APP,
        suppressWithinMs = 30_000
    )

    NoticeCode.BACKUP_IMPORT_COMPLETED -> NoticeCodePolicy(
        NoticeTopic.BACKUP,
        NoticeLevel.SUCCESS,
        DeliveryPolicy.PREFER_IN_APP,
        suppressWithinMs = 30_000
    )

    NoticeCode.BACKUP_IMPORT_FAILED -> NoticeCodePolicy(
        NoticeTopic.BACKUP,
        NoticeLevel.ERROR,
        DeliveryPolicy.PREFER_IN_APP,
        suppressWithinMs = 30_000
    )

    NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED -> NoticeCodePolicy(
        NoticeTopic.BACKUP,
        NoticeLevel.SUCCESS,
        DeliveryPolicy.IN_APP_ONLY,
        suppressWithinMs = 10_000
    )

    NoticeCode.BACKUP_DIRECTORY_CHECK_FAILED -> NoticeCodePolicy(
        NoticeTopic.BACKUP,
        NoticeLevel.ERROR,
        DeliveryPolicy.IN_APP_ONLY,
        suppressWithinMs = 10_000
    )

    NoticeCode.SECURITY_KEY_INVALIDATED -> NoticeCodePolicy(
        NoticeTopic.SECURITY,
        NoticeLevel.CRITICAL,
        DeliveryPolicy.PREFER_SYSTEM,
        mandatory = true
    )

    NoticeCode.SECURITY_RECOVERY_REQUIRED -> NoticeCodePolicy(
        NoticeTopic.SECURITY,
        NoticeLevel.CRITICAL,
        DeliveryPolicy.PREFER_IN_APP,
        mandatory = true
    )

    NoticeCode.SECURITY_ACTION_FAILED -> NoticeCodePolicy(
        NoticeTopic.SECURITY,
        NoticeLevel.ERROR,
        DeliveryPolicy.PREFER_SYSTEM,
        mandatory = true
    )

    NoticeCode.DATABASE_INDEX_REBUILD_COMPLETED -> NoticeCodePolicy(
        NoticeTopic.DATABASE,
        NoticeLevel.SUCCESS,
        DeliveryPolicy.PREFER_IN_APP,
        suppressWithinMs = 30_000
    )

    NoticeCode.DATABASE_INDEX_REBUILD_FAILED -> NoticeCodePolicy(
        NoticeTopic.DATABASE,
        NoticeLevel.ERROR,
        DeliveryPolicy.PREFER_IN_APP,
        suppressWithinMs = 30_000
    )

    NoticeCode.DATABASE_OPERATION_FAILED -> NoticeCodePolicy(
        NoticeTopic.DATABASE,
        NoticeLevel.ERROR,
        DeliveryPolicy.PREFER_IN_APP,
        mandatory = true
    )

    NoticeCode.NOTIFICATION_PERMISSION_DENIED -> NoticeCodePolicy(
        NoticeTopic.APP_LIFECYCLE,
        NoticeLevel.WARNING,
        DeliveryPolicy.IN_APP_ONLY
    )
}
