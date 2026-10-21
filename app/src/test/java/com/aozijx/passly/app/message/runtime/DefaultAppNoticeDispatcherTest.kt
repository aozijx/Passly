package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.app.message.model.NoticeCode
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
