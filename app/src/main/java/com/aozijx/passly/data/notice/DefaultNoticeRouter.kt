package com.aozijx.passly.data.notice

import com.aozijx.passly.domain.notice.model.AppNotice
import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.port.NoticeCodePolicy
import com.aozijx.passly.domain.notice.port.NoticeRoutePlan
import com.aozijx.passly.domain.notice.port.NoticeRouter
import com.aozijx.passly.domain.notice.port.NoticeRoutingContext
import com.aozijx.passly.domain.notice.port.NoticeTarget
import com.aozijx.passly.domain.notice.port.RouteReason
import javax.inject.Inject

/**
 * 默认路由判定实现。
 *
 * 判定顺序（纯函数，无副作用）：
 * 1. 必要消息（mandatory）跳过所有内容过滤
 * 2. 可选消息 → optionalMessagesEnabled 检查
 * 3. 可选消息 → topic.enabled 检查
 * 4. 可选消息 → topic.minimumLevel 检查
 * 5. 选择目标：IN_APP 总是可用；SYSTEM 需 check 设置 + 权限
 * 6. 返回 [NoticeRoutePlan]
 */
class DefaultNoticeRouter @Inject constructor() : NoticeRouter {

    override fun route(
        notice: AppNotice,
        policy: NoticeCodePolicy,
        context: NoticeRoutingContext
    ): NoticeRoutePlan {
        // --- 内容过滤 ---
        if (!policy.mandatory) {
            // 总开关
            if (!context.settings.optionalMessagesEnabled) {
                return NoticeRoutePlan.suppressed(RouteReason.MASTER_DISABLED)
            }
            // 分类开关
            val topicSetting = context.settings.topicSettings[notice.topic]
            if (topicSetting != null && !topicSetting.enabled) {
                return NoticeRoutePlan.suppressed(RouteReason.TOPIC_DISABLED)
            }
            // 最低级别
            val minLevel = topicSetting?.minimumLevel ?: NoticeLevel.INFO
            if (notice.level.ordinal < minLevel.ordinal) {
                return NoticeRoutePlan.suppressed(RouteReason.BELOW_MINIMUM_LEVEL)
            }
        }

        // --- 目标选择 ---
        val targets = mutableSetOf(NoticeTarget.IN_APP)

        val wantSystem = policy.deliveryPolicy.systemNotificationRequired ||
                notice.deliveryPolicy.systemNotificationRequired

        if (wantSystem) {
            if (context.settings.systemNotificationsEnabled) {
                // 权限检查由 Sink 层在 deliver 时执行
                targets.add(NoticeTarget.SYSTEM)
            } else {
                // 系统通知关闭：IN_APP 做 fallback
                return NoticeRoutePlan(
                    targets = targets,
                    reason = RouteReason.SYSTEM_DISABLED
                )
            }
        }

        return NoticeRoutePlan.allowed(targets)
    }
}
