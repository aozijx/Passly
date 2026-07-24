package com.aozijx.passly.domain.notice.port

import com.aozijx.passly.domain.notice.model.DeliveryPolicy
import com.aozijx.passly.domain.notice.model.NoticeCode

/**
 * NoticeCode 的可信策略注册表。
 * 每个 [NoticeCode] 关联一个 [NoticeCodePolicy]，发布时由 [NoticeRouter] 查询。
 */
data class NoticeCodePolicy(
    val deliveryPolicy: DeliveryPolicy,
    /** 必要消息不受 optionalMessagesEnabled 控制和 topic 开关影响 */
    val mandatory: Boolean = false,
    /** eventId 去重 TTL，默认 2 分钟 */
    val eventIdTtlMs: Long = 2 * 60 * 1000L
)

fun interface NoticeCodeRegistry {
    fun policyFor(code: NoticeCode): NoticeCodePolicy
}

fun defaultNoticeCodePolicy(code: NoticeCode): NoticeCodePolicy = when (code) {
    // Clipboard
    NoticeCode.CLIPBOARD_CLEARED -> NoticeCodePolicy(
        DeliveryPolicy(suppressWithinMs = 5_000)
    )

    NoticeCode.CLIPBOARD_CLEAR_FAILED -> NoticeCodePolicy(
        DeliveryPolicy(suppressWithinMs = 5_000)
    )
    // App lifecycle
    NoticeCode.APP_LOCKED -> NoticeCodePolicy(
        DeliveryPolicy(suppressWithinMs = 10_000)
    )

    NoticeCode.APP_CLOSE_REMINDER -> NoticeCodePolicy(
        DeliveryPolicy(suppressWithinMs = 10_000)
    )
    // Icon download
    NoticeCode.ICON_DOWNLOAD_COMPLETED -> NoticeCodePolicy(
        DeliveryPolicy(aggregateWithinMs = 5_000)
    )

    NoticeCode.ICON_DOWNLOAD_FAILED -> NoticeCodePolicy(
        DeliveryPolicy(aggregateWithinMs = 10_000)
    )
    // Backup
    NoticeCode.BACKUP_EXPORT_COMPLETED -> NoticeCodePolicy(
        DeliveryPolicy(suppressWithinMs = 30_000)
    )

    NoticeCode.BACKUP_EXPORT_FAILED -> NoticeCodePolicy(
        DeliveryPolicy(suppressWithinMs = 30_000)
    )

    NoticeCode.BACKUP_IMPORT_COMPLETED -> NoticeCodePolicy(
        DeliveryPolicy(suppressWithinMs = 30_000)
    )

    NoticeCode.BACKUP_IMPORT_FAILED -> NoticeCodePolicy(
        DeliveryPolicy(suppressWithinMs = 30_000)
    )
    // Security — mandatory for critical codes
    NoticeCode.SECURITY_KEY_INVALIDATED -> NoticeCodePolicy(
        DeliveryPolicy(systemNotificationRequired = true),
        mandatory = true
    )

    NoticeCode.SECURITY_RECOVERY_REQUIRED -> NoticeCodePolicy(
        DeliveryPolicy(),
        mandatory = true
    )

    NoticeCode.SECURITY_ACTION_FAILED -> NoticeCodePolicy(
        DeliveryPolicy(systemNotificationRequired = true),
        mandatory = true
    )
    // Database
    NoticeCode.DATABASE_INDEX_REBUILD_COMPLETED -> NoticeCodePolicy(
        DeliveryPolicy(suppressWithinMs = 30_000)
    )

    NoticeCode.DATABASE_INDEX_REBUILD_FAILED -> NoticeCodePolicy(
        DeliveryPolicy(suppressWithinMs = 30_000)
    )

    NoticeCode.DATABASE_OPERATION_FAILED -> NoticeCodePolicy(
        DeliveryPolicy(),
        mandatory = true
    )
}
