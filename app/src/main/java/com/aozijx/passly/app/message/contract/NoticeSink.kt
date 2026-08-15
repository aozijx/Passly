package com.aozijx.passly.app.message.contract

import com.aozijx.passly.app.message.model.AppNotice

sealed interface SinkResult {
    data object Delivered : SinkResult
    data object PermissionMissing : SinkResult
    data object Disabled : SinkResult
    data object AppNotVisible : SinkResult
    data class Failed(val errorCode: String) : SinkResult
}

/**
 * 消息输出端的统一接口。
 * 每个 [NoticeSink] 对应一个 [NoticeTarget]。
 */
interface NoticeSink {
    val target: NoticeTarget
    suspend fun deliver(notice: AppNotice): SinkResult
}
