package com.aozijx.passly.data.notice

import com.aozijx.passly.domain.notice.model.AppNotice
import com.aozijx.passly.domain.notice.port.NoticeTarget
import com.aozijx.passly.domain.notice.port.SinkResult
import com.aozijx.passly.domain.notice.port.SystemNotificationGateway
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubSystemNotificationGateway @Inject constructor() : SystemNotificationGateway {

    override val target: NoticeTarget get() = NoticeTarget.SYSTEM

    override suspend fun deliver(notice: AppNotice): SinkResult {
        // TODO: 实现真正的系统通知发送
        return SinkResult.Delivered
    }
}
