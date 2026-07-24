package com.aozijx.passly.domain.notice.port

import com.aozijx.passly.domain.notice.model.AppNotice

fun interface AppNoticePublisher {
    fun publish(notice: AppNotice)
}
