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
    /** 预占 eventId。返回 true 表示已被认领（应跳过）。 */
    fun claim(eventId: String): Boolean

    /** 分发完成后的善后。 */
    fun complete(eventId: String)

    /** 语义去重。返回 true 表示窗口内已存在同类消息（应跳过）。 */
    fun claimSemantic(code: NoticeCode): Boolean

    /** 清理过期条目。 */
    fun evict()
}
