package com.aozijx.passly.domain.notice.port

import com.aozijx.passly.domain.notice.model.AppNotice

data class NoticeDispatchReceipt(
    val eventId: String,
    val settingsVersion: Long,
    val plan: NoticeRoutePlan,
    val sinkResults: Map<NoticeTarget, SinkResult>,
    val status: NoticeDispatchStatus
)

enum class NoticeDispatchStatus {
    DELIVERED,
    PARTIALLY_DELIVERED,
    SUPPRESSED,
    DUPLICATE,
    FAILED
}

/**
 * 消息分发编排入口。
 *
 * 职责：
 * 1. 串行处理每条 [AppNotice]
 * 2. 原子读取设置快照
 * 3. 调用 [NoticeRouter] 获取路由计划
 * 4. 将路由计划分发给对应 [NoticeSink]
 * 5. 执行去重认领
 * 6. 返回 [NoticeDispatchReceipt]
 */
interface AppNoticeDispatcher {
    suspend fun dispatch(notice: AppNotice): NoticeDispatchReceipt
}
