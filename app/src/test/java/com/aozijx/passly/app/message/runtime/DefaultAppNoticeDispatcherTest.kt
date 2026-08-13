package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.data.message.model.AppMessageSettings
import com.aozijx.passly.data.message.model.NoticeCode
import com.aozijx.passly.data.message.model.newAppNotice
import com.aozijx.passly.app.message.contract.AppVisibility
import com.aozijx.passly.app.message.contract.AppVisibilityProvider
import com.aozijx.passly.app.message.contract.MessageSettingsSnapshotProvider
import com.aozijx.passly.app.message.contract.NoticeCodeRegistry
import com.aozijx.passly.app.message.contract.NoticeDispatchStatus
import com.aozijx.passly.app.message.contract.NoticeSink
import com.aozijx.passly.app.message.contract.NoticeTarget
import com.aozijx.passly.app.message.contract.SinkResult
import com.aozijx.passly.app.message.contract.SystemNotificationState
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import com.aozijx.passly.app.message.contract.VersionedMessageSettings
import com.aozijx.passly.app.message.contract.defaultNoticeCodePolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAppNoticeDispatcherTest {
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
                VersionedMessageSettings(9, AppMessageSettings())
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
            newAppNotice(NoticeCode.ICON_DOWNLOAD_COMPLETED, eventId = "icon-1")
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
                VersionedMessageSettings(1, AppMessageSettings())
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
                VersionedMessageSettings(
                    3,
                    AppMessageSettings(systemNotificationsEnabled = false)
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
            newAppNotice(NoticeCode.ICON_DOWNLOAD_COMPLETED, eventId = "icon-setting-off")
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
            notice: com.aozijx.passly.data.message.model.AppNotice
        ): SinkResult = delivery()
    }
}
