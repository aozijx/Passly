package com.aozijx.passly.app.message.contract

import com.aozijx.passly.app.message.model.AppNotice

fun interface AppNoticePublisher {
    fun publish(notice: AppNotice): NoticeEnqueueResult
}

sealed interface NoticeEnqueueResult {
    data object Accepted : NoticeEnqueueResult
    data object QueueFull : NoticeEnqueueResult
}
