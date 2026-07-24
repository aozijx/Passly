package com.aozijx.passly.domain.notice.port

import com.aozijx.passly.domain.notice.model.AppNotice

sealed interface SinkResult {
    data object Delivered : SinkResult
    data object PermissionMissing : SinkResult
    data object Disabled : SinkResult
    data class Failed(val error: Throwable?) : SinkResult
}

/**
 * 消息输出端的统一接口。
 * 每个 [NoticeSink] 对应一个 [NoticeTarget]。
 */
interface NoticeSink {
    val target: NoticeTarget
    suspend fun deliver(notice: AppNotice): SinkResult
}
