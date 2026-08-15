package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.app.message.model.AppNotice
import com.aozijx.passly.app.message.contract.AppNoticeDispatcher
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.app.message.contract.AppVisibilityProvider
import com.aozijx.passly.app.message.contract.DeduplicationClaim
import com.aozijx.passly.app.message.contract.MessageSettingsSnapshotProvider
import com.aozijx.passly.app.message.contract.NoticeCodeRegistry
import com.aozijx.passly.app.message.contract.NoticeDeduplicator
import com.aozijx.passly.app.message.contract.NoticeDispatchReceipt
import com.aozijx.passly.app.message.contract.NoticeDispatchStatus
import com.aozijx.passly.app.message.contract.NoticeEnqueueResult
import com.aozijx.passly.app.message.contract.NoticeRoutePlan
import com.aozijx.passly.app.message.contract.NoticeRouter
import com.aozijx.passly.app.message.contract.NoticeRoutingContext
import com.aozijx.passly.app.message.contract.NoticeSink
import com.aozijx.passly.app.message.contract.NoticeTarget
import com.aozijx.passly.app.message.contract.RouteReason
import com.aozijx.passly.app.message.contract.SinkResult
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class DefaultAppNoticeDispatcher @Inject constructor(
    private val router: NoticeRouter,
    private val deduplicator: NoticeDeduplicator,
    private val codeRegistry: NoticeCodeRegistry,
    private val settingsProvider: MessageSettingsSnapshotProvider,
    private val visibilityProvider: AppVisibilityProvider,
    private val systemStateProvider: SystemNotificationStateProvider,
    sinks: Set<@JvmSuppressWildcards NoticeSink>
) : AppNoticeDispatcher, AppNoticePublisher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val channel = Channel<AppNotice>(capacity = 128)
    private val dispatchMutex = Mutex()
    private val sinksByTarget = sinks.associateBy(NoticeSink::target)

    init {
        scope.launch {
            for (notice in channel) dispatch(notice)
        }
    }

    override fun publish(notice: AppNotice): NoticeEnqueueResult =
        if (channel.trySend(notice).isSuccess) {
            NoticeEnqueueResult.Accepted
        } else {
            NoticeEnqueueResult.QueueFull
        }

    override suspend fun dispatch(notice: AppNotice): NoticeDispatchReceipt =
        dispatchMutex.withLock { dispatchSerial(notice) }

    private suspend fun dispatchSerial(notice: AppNotice): NoticeDispatchReceipt {
        val policy = codeRegistry.policyFor(notice.code)
        val settings = settingsProvider.current()
        val systemState = systemStateProvider.current().copy(
            userSettingEnabled = settings.value.systemNotificationsEnabled
        )
        val context = NoticeRoutingContext(
            settings = settings.value,
            settingsVersion = settings.version,
            appVisibility = visibilityProvider.current(),
            systemNotificationState = systemState
        )
        val claim = deduplicator.begin(notice.eventId, policy.eventIdTtlMs)
        if (claim is DeduplicationClaim.Duplicate) {
            return receipt(
                notice,
                settings.version,
                NoticeRoutePlan.suppressed(RouteReason.SUPPRESSED_BY_DEDUP),
                emptyMap(),
                NoticeDispatchStatus.DUPLICATE
            )
        }
        claim as DeduplicationClaim.Acquired

        try {
            val plan = router.route(notice, policy, context)
            if (plan.targets.isEmpty()) {
                deduplicator.complete(claim)
                return receipt(
                    notice,
                    settings.version,
                    plan,
                    emptyMap(),
                    NoticeDispatchStatus.SUPPRESSED
                )
            }
            if (
                policy.suppressWithinMs > 0 &&
                deduplicator.claimSemantic(notice.code, policy.suppressWithinMs)
            ) {
                deduplicator.complete(claim)
                return receipt(
                    notice,
                    settings.version,
                    NoticeRoutePlan.suppressed(RouteReason.SUPPRESSED_BY_DEDUP),
                    emptyMap(),
                    NoticeDispatchStatus.SUPPRESSED
                )
            }

            val results = linkedMapOf<NoticeTarget, SinkResult>()
            for (target in plan.targets) {
                results[target] = deliver(target, notice)
            }
            val systemResult = results[NoticeTarget.SYSTEM]
            val fallbackTarget = plan.fallbackTarget
            if (
                fallbackTarget != null &&
                systemResult != null &&
                systemResult !is SinkResult.Delivered
            ) {
                results[fallbackTarget] = deliver(fallbackTarget, notice)
            }
            deduplicator.complete(claim)
            val delivered = results.values.count { it is SinkResult.Delivered }
            val status = when {
                delivered == results.size && delivered > 0 -> NoticeDispatchStatus.DELIVERED
                delivered > 0 -> NoticeDispatchStatus.PARTIALLY_DELIVERED
                else -> NoticeDispatchStatus.FAILED
            }
            return receipt(notice, settings.version, plan, results, status)
        } catch (cancelled: CancellationException) {
            deduplicator.release(claim)
            throw cancelled
        } catch (_: Throwable) {
            deduplicator.release(claim)
            return receipt(
                notice,
                settings.version,
                NoticeRoutePlan.suppressed(RouteReason.NO_AVAILABLE_TARGET),
                emptyMap(),
                NoticeDispatchStatus.FAILED
            )
        }
    }

    private suspend fun deliver(target: NoticeTarget, notice: AppNotice): SinkResult =
        sinksByTarget[target]?.deliver(notice)
            ?: SinkResult.Failed("notice.sink_missing")

    private fun receipt(
        notice: AppNotice,
        settingsVersion: Long,
        plan: NoticeRoutePlan,
        results: Map<NoticeTarget, SinkResult>,
        status: NoticeDispatchStatus
    ) = NoticeDispatchReceipt(
        eventId = notice.eventId,
        settingsVersion = settingsVersion,
        plan = plan,
        sinkResults = results,
        status = status
    )
}
