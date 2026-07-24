package com.aozijx.passly.data.notice

import com.aozijx.passly.domain.notice.model.NoticeCode
import com.aozijx.passly.domain.notice.port.NoticeDeduplicator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 默认消息去重实现。
 *
 * 两层策略：
 * - eventId 精确去重：最多 1024 条目，TTL 2 分钟
 * - 语义去重：按 [NoticeCode] + 窗口时间，窗口由调用方传入
 */
@Singleton
class DefaultNoticeDeduplicator @Inject constructor() : NoticeDeduplicator {

    private val claimedIds = object : LinkedHashMap<String, Long>(1024, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean {
            return size > MAX_IDS
        }
    }

    private val semanticMap = mutableMapOf<NoticeCode, Long>()

    @Synchronized
    override fun claim(eventId: String): Boolean {
        evict()
        val now = System.currentTimeMillis()
        val deadline = claimedIds[eventId]
        if (deadline != null && now < deadline) {
            return true // 已被认领且未过期
        }
        // 预占：先设一个远期的 deadline，complete 时更新为真实 deadline
        claimedIds[eventId] = now + 2 * 60 * 1000L
        return false
    }

    @Synchronized
    override fun complete(eventId: String) {
        // 保持现有 deadline（由 claim 设置的 TTL）
    }

    @Synchronized
    override fun claimSemantic(code: NoticeCode): Boolean {
        val now = System.currentTimeMillis()
        val deadline = semanticMap[code]
        return if (deadline != null && now < deadline) {
            true
        } else {
            semanticMap[code] = now + DEFAULT_SEMANTIC_WINDOW_MS
            false
        }
    }

    /** 使用指定的窗口进行语义去重。 */
    @Synchronized
    fun claimSemantic(code: NoticeCode, windowMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val deadline = semanticMap[code]
        return if (deadline != null && now < deadline) {
            true
        } else {
            semanticMap[code] = now + windowMs
            false
        }
    }

    @Synchronized
    override fun evict() {
        val now = System.currentTimeMillis()
        claimedIds.entries.removeAll { (_, deadline) -> now >= deadline }
        semanticMap.entries.removeAll { (_, deadline) -> now >= deadline }
    }

    /**
     * 获取语义去重窗口。
     * Dispatch 时根据 DeliceryPolicy.suppressWithinMs 传入具体窗口。
     */
    fun getSemanticDeadline(code: NoticeCode): Long? = semanticMap[code]

    companion object {
        const val MAX_IDS = 1024

        /** 默认语义去重窗口 5 秒 */
        private const val DEFAULT_SEMANTIC_WINDOW_MS = 5000L
    }
}
