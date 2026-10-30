package com.aozijx.passly.app.message.contract

/**
 * 消息去重。
 *
 * eventId 精确去重，防止同一业务事件重复投递。
 *
 * 两阶段 claim 阻止分发重入；至少一个目标成功后 complete，全部失败或取消时 release。
 */
interface NoticeDeduplicator {
    fun begin(eventId: String, ttlMs: Long): DeduplicationClaim

    fun complete(claim: DeduplicationClaim.Acquired)

    fun release(claim: DeduplicationClaim.Acquired)
}

sealed interface DeduplicationClaim {
    data class Acquired(
        val eventId: String,
        val token: Long,
        val ttlMs: Long
    ) : DeduplicationClaim

    data object Duplicate : DeduplicationClaim
}
