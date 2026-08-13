package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.domain.notice.model.AppNotice
import com.aozijx.passly.domain.notice.model.DeliveryPolicy
import com.aozijx.passly.domain.notice.port.AppVisibility
import com.aozijx.passly.domain.notice.port.NoticeCodePolicy
import com.aozijx.passly.domain.notice.port.NoticeRoutePlan
import com.aozijx.passly.domain.notice.port.NoticeRouter
import com.aozijx.passly.domain.notice.port.NoticeRoutingContext
import com.aozijx.passly.domain.notice.port.NoticeTarget
import com.aozijx.passly.domain.notice.port.RouteReason
import javax.inject.Inject

class DefaultNoticeRouter @Inject constructor() : NoticeRouter {
    override fun route(
        notice: AppNotice,
        policy: NoticeCodePolicy,
        context: NoticeRoutingContext
    ): NoticeRoutePlan {
        if (!policy.mandatory) {
            if (!context.settings.optionalMessagesEnabled) {
                return NoticeRoutePlan.suppressed(RouteReason.MASTER_DISABLED)
            }
            val topic = context.settings.topic(policy.topic)
            if (!topic.enabled) {
                return NoticeRoutePlan.suppressed(RouteReason.TOPIC_DISABLED)
            }
            if (policy.level.ordinal < topic.minimumLevel.ordinal) {
                return NoticeRoutePlan.suppressed(RouteReason.BELOW_MINIMUM_LEVEL)
            }
        }

        val foreground = context.appVisibility == AppVisibility.FOREGROUND
        val initialTargets = when (policy.deliveryPolicy) {
            DeliveryPolicy.IN_APP_ONLY -> setOf(NoticeTarget.IN_APP)
            DeliveryPolicy.SYSTEM_ONLY -> setOf(NoticeTarget.SYSTEM)
            DeliveryPolicy.PREFER_IN_APP ->
                setOf(if (foreground) NoticeTarget.IN_APP else NoticeTarget.SYSTEM)
            DeliveryPolicy.PREFER_SYSTEM -> setOf(NoticeTarget.SYSTEM)
            DeliveryPolicy.BOTH -> buildSet {
                if (foreground) add(NoticeTarget.IN_APP)
                add(NoticeTarget.SYSTEM)
            }
        }

        if (NoticeTarget.IN_APP in initialTargets && !foreground) {
            return NoticeRoutePlan.suppressed(RouteReason.APP_NOT_VISIBLE)
        }
        if (NoticeTarget.SYSTEM !in initialTargets) return NoticeRoutePlan.allowed(initialTargets)
        if (context.systemNotificationState.available) {
            return NoticeRoutePlan(
                targets = initialTargets,
                reason = RouteReason.ALLOWED,
                fallbackTarget = NoticeTarget.IN_APP.takeIf {
                    foreground && policy.deliveryPolicy == DeliveryPolicy.PREFER_SYSTEM
                }
            )
        }

        val reason = when {
            !context.systemNotificationState.userSettingEnabled ->
                RouteReason.SYSTEM_DISABLED
            !context.systemNotificationState.runtimePermissionGranted ->
                RouteReason.SYSTEM_PERMISSION_MISSING
            !context.systemNotificationState.channelEnabled ->
                RouteReason.SYSTEM_CHANNEL_DISABLED
            else -> RouteReason.SYSTEM_DISABLED
        }
        val canFallback = foreground && policy.deliveryPolicy != DeliveryPolicy.SYSTEM_ONLY
        return if (canFallback) {
            NoticeRoutePlan(
                targets = (initialTargets - NoticeTarget.SYSTEM) + NoticeTarget.IN_APP,
                reason = RouteReason.FALLBACK_TO_IN_APP
            )
        } else {
            NoticeRoutePlan.suppressed(reason)
        }
    }
}
