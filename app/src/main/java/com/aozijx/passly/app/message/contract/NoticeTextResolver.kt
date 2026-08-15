package com.aozijx.passly.app.message.contract

import com.aozijx.passly.app.message.model.AppNotice
import com.aozijx.passly.app.message.model.NoticeLevel

data class ResolvedNotice(
    val eventId: String,
    val title: String,
    val text: String,
    val level: NoticeLevel
)

fun interface NoticeTextResolver {
    fun resolve(notice: AppNotice): ResolvedNotice
}
