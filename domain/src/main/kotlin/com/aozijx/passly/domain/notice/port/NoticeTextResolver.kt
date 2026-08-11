package com.aozijx.passly.domain.notice.port

import com.aozijx.passly.domain.notice.model.AppNotice
import com.aozijx.passly.domain.notice.model.NoticeLevel

data class ResolvedNotice(
    val eventId: String,
    val title: String,
    val text: String,
    val level: NoticeLevel
)

fun interface NoticeTextResolver {
    fun resolve(notice: AppNotice): ResolvedNotice
}
