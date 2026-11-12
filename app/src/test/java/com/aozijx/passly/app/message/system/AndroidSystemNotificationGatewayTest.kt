package com.aozijx.passly.app.message.system

import android.app.NotificationManager
import com.aozijx.passly.R
import com.aozijx.passly.app.message.contract.ResolvedNotice
import com.aozijx.passly.app.message.contract.SinkResult
import com.aozijx.passly.app.message.model.AppNotice
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.NoticeLevel
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSystemNotificationGatewayTest {
    @Test
    fun `deliver creates channel resolves localized copy and sends stable notification`() =
        runBlocking {
            val platform = RecordingSystemNotificationPlatform()
            val notice = newAppNotice(NoticeCode.APP_LOCKED, eventId = "locked-event")
            var resolvedNotice: AppNotice? = null
            val gateway =
                gateway(platform) {
                    resolvedNotice = it
                    ResolvedNotice(it.eventId, "Passly", "Vault locked", NoticeLevel.INFO)
                }

            assertEquals(SinkResult.Delivered, gateway.deliver(notice))

            assertSame(notice, resolvedNotice)
            assertEquals(
                listOf(
                    "channel:${AndroidSystemNotificationGateway.CHANNEL_ID}",
                    "notify:${AndroidSystemNotificationGateway.CHANNEL_ID}",
                ),
                platform.calls,
            )
            assertEquals(R.string.notification_channel_app_messages, platform.channelNameResourceId)
            assertEquals(R.string.notification_channel_app_messages_summary, platform.channelDescriptionResourceId)
            assertEquals(NotificationManager.IMPORTANCE_DEFAULT, platform.channelImportance)
            assertEquals(AndroidSystemNotificationGateway.notificationId(notice.eventId), platform.notificationId)
            assertEquals("Passly", platform.title)
            assertEquals("Vault locked", platform.text)
            assertEquals(R.mipmap.launcher_logo, platform.smallIconResourceId)
        }

    @Test
    fun `deliver maps ordinary platform and text failures to failed result`() =
        runBlocking {
            val platformFailure = IOException("platform")
            val createFailure =
                gateway(RecordingSystemNotificationPlatform().apply { createFailure = platformFailure }) {
                    resolved(it)
                }
            assertEquals(
                SinkResult.Failed("notice.system_delivery_failed"),
                createFailure.deliver(newAppNotice(NoticeCode.APP_LOCKED)),
            )

            val textFailure = IOException("resolver")
            val resolveFailure = gateway(RecordingSystemNotificationPlatform()) { throw textFailure }
            assertEquals(
                SinkResult.Failed("notice.system_delivery_failed"),
                resolveFailure.deliver(newAppNotice(NoticeCode.APP_LOCKED)),
            )

            val notifyFailure = IOException("notify")
            val notifyPlatform = RecordingSystemNotificationPlatform().apply { this.notifyFailure = notifyFailure }
            val notifyGateway = gateway(notifyPlatform, ::resolved)
            assertEquals(
                SinkResult.Failed("notice.system_delivery_failed"),
                notifyGateway.deliver(newAppNotice(NoticeCode.APP_LOCKED)),
            )
        }

    @Test
    fun `deliver propagates cancellation and fatal errors`() {
        val cancelled = CancellationException("cancelled")
        val cancelGateway = gateway(RecordingSystemNotificationPlatform()) { throw cancelled }
        assertSame(cancelled, assertThrows(CancellationException::class.java) {
            runBlocking { cancelGateway.deliver(newAppNotice(NoticeCode.APP_LOCKED)) }
        })

        val fatal = LinkageError("fatal")
        val fatalPlatform = RecordingSystemNotificationPlatform().apply { notifyFailure = fatal }
        val fatalGateway = gateway(fatalPlatform, ::resolved)
        assertSame(fatal, assertThrows(LinkageError::class.java) {
            runBlocking { fatalGateway.deliver(newAppNotice(NoticeCode.APP_LOCKED)) }
        })
    }

    @Test
    fun `deliver does not touch channel or resolver while notifications are unavailable`() =
        runBlocking {
            val platform = RecordingSystemNotificationPlatform().apply {
                channelImportance = NotificationManager.IMPORTANCE_NONE
            }
            var resolverCalled = false
            val gateway = gateway(platform) {
                resolverCalled = true
                resolved(it)
            }

            assertEquals(SinkResult.Disabled, gateway.deliver(newAppNotice(NoticeCode.APP_LOCKED)))
            assertTrue(platform.calls.isEmpty())
            assertFalse(resolverCalled)
        }

    private fun gateway(
        platform: RecordingSystemNotificationPlatform,
        resolve: (AppNotice) -> ResolvedNotice,
    ) =
        AndroidSystemNotificationGateway(
            platform = platform,
            permissionStatusReader = { PermissionStatus.GRANTED },
            textResolver = resolve,
        )

    private fun resolved(notice: AppNotice) =
        ResolvedNotice(notice.eventId, "title", "text", NoticeLevel.INFO)

    private class RecordingSystemNotificationPlatform : SystemNotificationPlatform {
        val calls = mutableListOf<String>()
        var enabled = true
        var channelImportance: Int? = null
        var createFailure: Throwable? = null
        var notifyFailure: Throwable? = null
        var channelNameResourceId: Int? = null
        var channelDescriptionResourceId: Int? = null
        var notificationId: Int? = null
        var title: String? = null
        var text: String? = null
        var smallIconResourceId: Int? = null

        override fun areNotificationsEnabled(): Boolean = enabled

        override fun channelImportance(channelId: String): Int? = channelImportance

        override fun createChannel(
            channelId: String,
            nameResourceId: Int,
            descriptionResourceId: Int,
            importance: Int,
        ) {
            calls += "channel:$channelId"
            channelNameResourceId = nameResourceId
            channelDescriptionResourceId = descriptionResourceId
            this.channelImportance = importance
            createFailure?.let { throw it }
        }

        override fun notify(
            channelId: String,
            notificationId: Int,
            title: String,
            text: String,
            smallIconResourceId: Int,
        ) {
            calls += "notify:$channelId"
            this.notificationId = notificationId
            this.title = title
            this.text = text
            this.smallIconResourceId = smallIconResourceId
            notifyFailure?.let { throw it }
        }
    }
}
