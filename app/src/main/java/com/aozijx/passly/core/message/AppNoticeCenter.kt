package com.aozijx.passly.core.message

import com.aozijx.passly.domain.notice.model.AppNotice
import com.aozijx.passly.domain.notice.model.DeliveryHint
import com.aozijx.passly.domain.notice.model.NoticeContent
import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.model.NoticeTopic
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * AppNotice 发布中心。
 *
 * 区别于 [AppMessageCenter]（简单文本消息），[AppNoticeCenter] 承载结构化的事件通知，
 * 支持按话题（[NoticeTopic]）、级别（[NoticeLevel]）做精细化过滤。
 *
 * 所有发布到此中心的 [AppNotice] 会进入统一的消息宿主（[com.aozijx.passly.feature.message.AppMessageHostViewModel]）
 * 进行投递判定，判定逻辑由 [com.aozijx.passly.domain.notice.model.AppMessageSettings] 控制。
 */
object AppNoticeCenter {
    private val events = MutableSharedFlow<AppNotice>(extraBufferCapacity = 32)
    val notices: SharedFlow<AppNotice> = events.asSharedFlow()

    fun publish(notice: AppNotice) {
        events.tryEmit(notice)
    }

    fun publish(
        eventId: String,
        topic: NoticeTopic,
        level: NoticeLevel,
        text: String,
        title: String? = null,
        detail: String? = null,
        deliveryHint: DeliveryHint = DeliveryHint.AUTO,
        required: Boolean = false
    ) = publish(
        AppNotice(
            eventId = eventId,
            topic = topic,
            level = level,
            content = NoticeContent(title = title, text = text, detail = detail),
            deliveryHint = deliveryHint,
            required = required
        )
    )
}
