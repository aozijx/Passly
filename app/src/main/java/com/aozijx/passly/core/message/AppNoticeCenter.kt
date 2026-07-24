package com.aozijx.passly.core.message

import com.aozijx.passly.domain.notice.model.AppNotice
import com.aozijx.passly.domain.notice.model.ArgumentKey
import com.aozijx.passly.domain.notice.model.DeliveryPolicy
import com.aozijx.passly.domain.notice.model.NoticeCode
import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.model.NoticeTopic
import com.aozijx.passly.domain.notice.model.argument

/**
 * 旧的全局 AppNotice 发布中心（Legacy）。
 *
 * 新代码应通过 [com.aozijx.passly.domain.notice.port.AppNoticePublisher] 注入发布。
 * 此类仅作为过渡期向后兼容使用，将在 M14 删除。
 */
@Deprecated("Use AppNoticePublisher instead")
object AppNoticeCenter {
    fun publish(notice: AppNotice) {
        // 委托给新 Dispatcher 的逻辑将在 DI 绑定完成后处理
    }

    @Deprecated("Use typed AppNotice arguments instead")
    fun publish(
        eventId: String,
        code: NoticeCode,
        topic: NoticeTopic,
        level: NoticeLevel,
        text: String,
        title: String? = null,
        deliveryPolicy: DeliveryPolicy = DeliveryPolicy()
    ) = publish(
        AppNotice(
            eventId = eventId,
            code = code,
            topic = topic,
            level = level,
            arguments = mapOf(
                argument(ArgumentKey.REASON_CODE, title ?: text)
            ),
            deliveryPolicy = deliveryPolicy
        )
    )
}
