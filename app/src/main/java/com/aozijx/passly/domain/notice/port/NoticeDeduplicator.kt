package com.aozijx.passly.domain.notice.port

import com.aozijx.passly.domain.notice.model.NoticeCode

/**
 * 消息去重。
 *
 * 两层策略：
 * 1. [claim] — eventId 精确去重，防止同一事件重复投递
 * 2. [claimSemantic] — 按 [NoticeCode] + 时间窗口的语义合并
 *
 * 两阶段（claim → complete）保证在分发完成前阻止重入。
 */
interface NoticeDeduplicator {
    fun begin(eventId: String, ttlMs: Long): DeduplicationClaim

    fun complete(claim: DeduplicationClaim.Acquired)

    fun release(claim: DeduplicationClaim.Acquired)

    fun claimSemantic(code: NoticeCode, windowMs: Long): Boolean
}

sealed interface DeduplicationClaim {
    class Acquired internal constructor(
        val eventId: String,
        internal val token: Long,
        internal val ttlMs: Long
    ) : DeduplicationClaim

    data object Duplicate : DeduplicationClaim
}
