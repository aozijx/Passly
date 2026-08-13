package com.aozijx.passly.app.message.contract

/**
 * 系统通知网关。
 * 负责向系统状态栏发送通知。
 * 自身不缓存设置、不参与路由判断，只执行"如何发送"。
 */
interface SystemNotificationGateway : NoticeSink {
    override val target: NoticeTarget get() = NoticeTarget.SYSTEM
}
