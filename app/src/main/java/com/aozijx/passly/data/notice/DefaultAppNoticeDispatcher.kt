package com.aozijx.passly.data.notice

import com.aozijx.passly.domain.notice.model.AppMessageSettings
import com.aozijx.passly.domain.notice.model.AppNotice
import com.aozijx.passly.domain.notice.port.AppNoticeDispatcher
import com.aozijx.passly.domain.notice.port.AppNoticePublisher
import com.aozijx.passly.domain.notice.port.InAppNoticeStream
import com.aozijx.passly.domain.notice.port.NoticeCodeRegistry
import com.aozijx.passly.domain.notice.port.NoticeDispatchReceipt
import com.aozijx.passly.domain.notice.port.NoticeRoutePlan
import com.aozijx.passly.domain.notice.port.NoticeRouter
import com.aozijx.passly.domain.notice.port.NoticeRoutingContext
import com.aozijx.passly.domain.notice.port.NoticeTarget
import com.aozijx.passly.domain.notice.port.RouteReason
import com.aozijx.passly.domain.notice.port.SinkResult
import com.aozijx.passly.domain.notice.port.SystemNotificationGateway
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 默认消息分发编排器。
 *
 * 架构：
 * - 实现 [AppNoticePublisher] 作为入口
 * - 实现 [InAppNoticeStream] 作为 UI 订阅流
 * - 内部串行处理，每条消息原子读一次设置快照
 */
@Singleton
class DefaultAppNoticeDispatcher @Inject constructor(
    private val router: NoticeRouter,
    private val deduplicator: DefaultNoticeDeduplicator,
    private val codeRegistry: NoticeCodeRegistry,
    private val settingsRepository: AppSettingsRepository,
    private val systemNotificationGateway: SystemNotificationGateway
) : AppNoticeDispatcher, AppNoticePublisher, InAppNoticeStream {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val channel = Channel<AppNotice>(Channel.UNLIMITED)
    private val _notices = MutableSharedFlow<AppNotice>(extraBufferCapacity = 64)

    @Volatile
    private var cachedSettings: AppMessageSettings = AppMessageSettings()

    override val notices = _notices.asSharedFlow()

    init {
        scope.launch {
            settingsRepository.settings
                .map { it.notifications.appMessageSettings }
                .collect { cachedSettings = it }
        }
        scope.launch {
            for (notice in channel) {
                dispatch(notice)
            }
        }
    }

    override fun publish(notice: AppNotice) {
        channel.trySend(notice)
    }

    override suspend fun dispatch(notice: AppNotice): NoticeDispatchReceipt {
        // 1. 使用缓存的设置快照
        val context = NoticeRoutingContext(
            settings = cachedSettings,
            isForeground = true // FIXME: 接入 Activity lifecycle
        )

        // 2. 查找策略
        val policy = codeRegistry.policyFor(notice.code)

        // 3. eventId 去重
        if (deduplicator.claim(notice.eventId)) {
            return NoticeDispatchReceipt(
                eventId = notice.eventId,
                plan = NoticeRoutePlan.suppressed(RouteReason.SUPPRESSED_BY_DEDUP),
                sinkResults = emptyMap()
            )
        }

        // 4. 语义去重
        val suppressWindow = policy.deliveryPolicy.suppressWithinMs
            .coerceAtLeast(notice.deliveryPolicy.suppressWithinMs)
        if (suppressWindow > 0 && deduplicator.claimSemantic(notice.code, suppressWindow)) {
            deduplicator.complete(notice.eventId)
            return NoticeDispatchReceipt(
                eventId = notice.eventId,
                plan = NoticeRoutePlan.suppressed(RouteReason.SUPPRESSED_BY_DEDUP),
                sinkResults = emptyMap()
            )
        }

        // 5. 路由
        val plan = router.route(notice, policy, context)

        // 6. 执行 Sink
        if (plan.targets.isNotEmpty()) {
            val sinkResults = mutableMapOf<NoticeTarget, SinkResult>()

            if (plan.targets.contains(NoticeTarget.IN_APP)) {
                _notices.tryEmit(notice)
                sinkResults[NoticeTarget.IN_APP] = SinkResult.Delivered
            }

            if (plan.targets.contains(NoticeTarget.SYSTEM)) {
                val result = systemNotificationGateway.deliver(notice)
                sinkResults[NoticeTarget.SYSTEM] = result
            }

            deduplicator.complete(notice.eventId)

            return NoticeDispatchReceipt(
                eventId = notice.eventId,
                plan = plan,
                sinkResults = sinkResults
            )
        }

        // 7. 被抑制
        deduplicator.complete(notice.eventId)
        return NoticeDispatchReceipt(
            eventId = notice.eventId,
            plan = plan,
            sinkResults = emptyMap()
        )
    }
}
