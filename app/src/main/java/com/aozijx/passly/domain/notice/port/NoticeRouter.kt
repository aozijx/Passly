package com.aozijx.passly.domain.notice.port

import com.aozijx.passly.domain.notice.model.AppNotice

enum class NoticeTarget { IN_APP, SYSTEM }

enum class RouteReason {
    ALLOWED,
    MASTER_DISABLED,
    TOPIC_DISABLED,
    BELOW_MINIMUM_LEVEL,
    SUPPRESSED_BY_DEDUP,
    SYSTEM_PERMISSION_MISSING,
    SYSTEM_DISABLED,
    SYSTEM_CHANNEL_DISABLED,
    APP_NOT_VISIBLE,
    FALLBACK_TO_IN_APP,
    NO_AVAILABLE_TARGET
}

data class NoticeRoutePlan(
    val targets: Set<NoticeTarget>,
    val reason: RouteReason,
    val fallbackTarget: NoticeTarget? = null
) {
    companion object {
        fun suppressed(reason: RouteReason) = NoticeRoutePlan(
            targets = emptySet(),
            reason = reason
        )

        fun allowed(targets: Set<NoticeTarget>) = NoticeRoutePlan(
            targets = targets,
            reason = RouteReason.ALLOWED
        )
    }
}

/**
 * 纯函数路由判定。
 * 根据 [AppNotice] 和 [NoticeRoutingContext] 决定消息应投递到哪些目标。
 * 不持有状态，不产生副作用。
 */
fun interface NoticeRouter {
    fun route(
        notice: AppNotice,
        policy: NoticeCodePolicy,
        context: NoticeRoutingContext
    ): NoticeRoutePlan
}
