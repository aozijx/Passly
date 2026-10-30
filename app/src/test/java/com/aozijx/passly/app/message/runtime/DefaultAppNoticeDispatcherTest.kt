package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.DeliveryPolicy
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.app.message.contract.AppVisibility
import com.aozijx.passly.app.message.contract.AppVisibilityProvider
import com.aozijx.passly.app.message.contract.MessageSettingsSnapshotProvider
import com.aozijx.passly.app.message.contract.MessageSettingsSnapshot
import com.aozijx.passly.app.message.contract.NoticeCodeRegistry
import com.aozijx.passly.app.message.contract.NoticeDispatchStatus
import com.aozijx.passly.app.message.contract.NoticeSink
import com.aozijx.passly.app.message.contract.NoticeTarget
import com.aozijx.passly.app.message.contract.RouteReason
import com.aozijx.passly.app.message.contract.SinkResult
import com.aozijx.passly.app.message.contract.SystemNotificationState
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import com.aozijx.passly.app.message.contract.defaultNoticeCodePolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAppNoticeDispatcherTest {
    @Test
    fun settingsUnavailableSuppressesNoticeWithoutClaimingOrDeliveringIt() = runBlocking {
        var deliveries = 0
        val dispatcher = DefaultAppNoticeDispatcher(
            router = DefaultNoticeRouter(),
            deduplicator = DefaultNoticeDeduplicator { 1_000L },
            codeRegistry = NoticeCodeRegistry(::defaultNoticeCodePolicy),
            settingsProvider = MessageSettingsSnapshotProvider { MessageSettingsSnapshot.Loading },
            visibilityProvider = AppVisibilityProvider { AppVisibility.FOREGROUND },
            systemStateProvider = availableSystemState(),
            sinks = setOf(
                sink(NoticeTarget.IN_APP) {
                    deliveries++
                    SinkResult.Delivered
                }
            )
        )

        val receipt = dispatcher.dispatch(
            newAppNotice(NoticeCode.CLIPBOARD_CLEARED, eventId = "settings-loading")
        )

        assertEquals(0, deliveries)
        assertEquals(0, receipt.settingsVersion)
        assertEquals(NoticeDispatchStatus.SUPPRESSED, receipt.status)
        assertEquals(RouteReason.SETTINGS_UNAVAILABLE, receipt.plan.reason)
    }

    @Test
    fun systemFailureFallsBackToInAppAndReturnsReceipt() = runBlocking {
        var settingsReads = 0
        val delivered = mutableListOf<NoticeTarget>()
        val dispatcher = DefaultAppNoticeDispatcher(
            router = DefaultNoticeRouter(),
            deduplicator = DefaultNoticeDeduplicator { 1_000L },
            codeRegistry = NoticeCodeRegistry(::defaultNoticeCodePolicy),
            settingsProvider = MessageSettingsSnapshotProvider {
                settingsReads++
                MessageSettingsSnapshot.Ready(9, MessageSettings())
            },
            visibilityProvider = AppVisibilityProvider { AppVisibility.FOREGROUND },
            systemStateProvider = availableSystemState(),
            sinks = setOf(
                sink(NoticeTarget.SYSTEM) {
                    SinkResult.Failed("notice.test_failure")
                },
                sink(NoticeTarget.IN_APP) {
                    delivered += NoticeTarget.IN_APP
                    SinkResult.Delivered
                }
            )
        )

        val receipt = dispatcher.dispatch(
            newAppNotice(NoticeCode.SECURITY_ACTION_FAILED, eventId = "security-1")
        )

        assertEquals(1, settingsReads)
        assertEquals(9, receipt.settingsVersion)
        assertEquals(NoticeDispatchStatus.PARTIALLY_DELIVERED, receipt.status)
        assertEquals(listOf(NoticeTarget.IN_APP), delivered)
    }

    @Test
    fun repeatedEventIdReturnsDuplicateWithoutSecondDelivery() = runBlocking {
        var deliveries = 0
        val dispatcher = DefaultAppNoticeDispatcher(
            router = DefaultNoticeRouter(),
            deduplicator = DefaultNoticeDeduplicator { 1_000L },
            codeRegistry = NoticeCodeRegistry(::defaultNoticeCodePolicy),
            settingsProvider = MessageSettingsSnapshotProvider {
                MessageSettingsSnapshot.Ready(1, MessageSettings())
            },
            visibilityProvider = AppVisibilityProvider { AppVisibility.FOREGROUND },
            systemStateProvider = availableSystemState(),
            sinks = setOf(
                sink(NoticeTarget.IN_APP) {
                    deliveries++
                    SinkResult.Delivered
                }
            )
        )
        val notice = newAppNotice(
            NoticeCode.CLIPBOARD_CLEARED,
            eventId = "clipboard-1"
        )

        dispatcher.dispatch(notice)
        val duplicate = dispatcher.dispatch(notice)

        assertEquals(1, deliveries)
        assertEquals(NoticeDispatchStatus.DUPLICATE, duplicate.status)
        assertTrue(duplicate.sinkResults.isEmpty())
    }

    @Test
    fun distinctEventIdsWithSameNoticeCodeBothDeliver() = runBlocking {
        var deliveries = 0
        val dispatcher = DefaultAppNoticeDispatcher(
            router = DefaultNoticeRouter(),
            deduplicator = DefaultNoticeDeduplicator { 1_000L },
            codeRegistry = NoticeCodeRegistry(::defaultNoticeCodePolicy),
            settingsProvider = MessageSettingsSnapshotProvider {
                MessageSettingsSnapshot.Ready(1, MessageSettings())
            },
            visibilityProvider = AppVisibilityProvider { AppVisibility.FOREGROUND },
            systemStateProvider = availableSystemState(),
            sinks = setOf(
                sink(NoticeTarget.IN_APP) {
                    deliveries++
                    SinkResult.Delivered
                }
            )
        )

        val first = dispatcher.dispatch(
            newAppNotice(NoticeCode.CLIPBOARD_CLEARED, eventId = "clipboard-clear-1")
        )
        val second = dispatcher.dispatch(
            newAppNotice(NoticeCode.CLIPBOARD_CLEARED, eventId = "clipboard-clear-2")
        )

        assertEquals(2, deliveries)
        assertEquals(NoticeDispatchStatus.DELIVERED, first.status)
        assertEquals(NoticeDispatchStatus.DELIVERED, second.status)
    }

    @Test
    fun failedDeliveryCanRetryWithTheSameEventId() = runBlocking {
        var attempts = 0
        val dispatcher = DefaultAppNoticeDispatcher(
            router = DefaultNoticeRouter(),
            deduplicator = DefaultNoticeDeduplicator { 1_000L },
            codeRegistry = NoticeCodeRegistry { code ->
                defaultNoticeCodePolicy(code).copy(deliveryPolicy = DeliveryPolicy.SYSTEM_ONLY)
            },
            settingsProvider = MessageSettingsSnapshotProvider {
                MessageSettingsSnapshot.Ready(1, MessageSettings())
            },
            visibilityProvider = AppVisibilityProvider { AppVisibility.FOREGROUND },
            systemStateProvider = availableSystemState(),
            sinks = setOf(
                sink(NoticeTarget.SYSTEM) {
                    attempts++
                    if (attempts == 1) SinkResult.Failed("notice.temporarily_unavailable")
                    else SinkResult.Delivered
                }
            )
        )
        val notice = newAppNotice(NoticeCode.SECURITY_ACTION_FAILED, eventId = "retry-security-1")

        val first = dispatcher.dispatch(notice)
        val second = dispatcher.dispatch(notice)

        assertEquals(2, attempts)
        assertEquals(NoticeDispatchStatus.FAILED, first.status)
        assertEquals(NoticeDispatchStatus.DELIVERED, second.status)
    }

    @Test
    fun appSystemNotificationSettingOverridesPlatformAvailability() = runBlocking {
        val delivered = mutableListOf<NoticeTarget>()
        val dispatcher = DefaultAppNoticeDispatcher(
            router = DefaultNoticeRouter(),
            deduplicator = DefaultNoticeDeduplicator { 1_000L },
            codeRegistry = NoticeCodeRegistry(::defaultNoticeCodePolicy),
            settingsProvider = MessageSettingsSnapshotProvider {
                MessageSettingsSnapshot.Ready(
                    3,
                    MessageSettings(systemNotificationsEnabled = false)
                )
            },
            visibilityProvider = AppVisibilityProvider { AppVisibility.FOREGROUND },
            systemStateProvider = availableSystemState(),
            sinks = setOf(
                sink(NoticeTarget.SYSTEM) {
                    delivered += NoticeTarget.SYSTEM
                    SinkResult.Delivered
                },
                sink(NoticeTarget.IN_APP) {
                    delivered += NoticeTarget.IN_APP
                    SinkResult.Delivered
                }
            )
        )

        val receipt = dispatcher.dispatch(
            newAppNotice(NoticeCode.SECURITY_ACTION_FAILED, eventId = "security-setting-off")
        )

        assertEquals(listOf(NoticeTarget.IN_APP), delivered)
        assertEquals(NoticeDispatchStatus.DELIVERED, receipt.status)
    }

    private fun availableSystemState() = SystemNotificationStateProvider {
        SystemNotificationState(
            userSettingEnabled = true,
            runtimePermissionGranted = true,
            notificationsEnabledBySystem = true,
            channelEnabled = true
        )
    }

    private fun sink(
        sinkTarget: NoticeTarget,
        delivery: suspend () -> SinkResult
    ) = object : NoticeSink {
        override val target: NoticeTarget = sinkTarget
        override suspend fun deliver(
            notice: com.aozijx.passly.app.message.model.AppNotice
        ): SinkResult = delivery()
    }
}
