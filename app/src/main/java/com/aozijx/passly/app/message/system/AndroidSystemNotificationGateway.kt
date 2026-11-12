package com.aozijx.passly.app.message.system

import android.app.NotificationManager
import com.aozijx.passly.R
import com.aozijx.passly.app.message.contract.NoticeTextResolver
import com.aozijx.passly.app.message.contract.SinkResult
import com.aozijx.passly.app.message.contract.SystemNotificationGateway
import com.aozijx.passly.app.message.contract.SystemNotificationState
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import com.aozijx.passly.app.message.model.AppNotice
import com.aozijx.passly.core.permission.contract.PermissionStatusReader
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class AndroidSystemNotificationGateway
    @Inject
    constructor(
        private val platform: SystemNotificationPlatform,
        private val permissionStatusReader: PermissionStatusReader,
        private val textResolver: NoticeTextResolver,
    ) : SystemNotificationGateway,
        SystemNotificationStateProvider {
        override fun current(): SystemNotificationState =
            SystemNotificationState(
                userSettingEnabled = true,
                runtimePermissionGranted =
                    permissionStatusReader.status(
                        RuntimePermission.POST_NOTIFICATIONS,
                    ) != PermissionStatus.DENIED,
                notificationsEnabledBySystem = platform.areNotificationsEnabled(),
                channelEnabled =
                    platform.channelImportance(CHANNEL_ID) != NotificationManager.IMPORTANCE_NONE,
            )

        override suspend fun deliver(notice: AppNotice): SinkResult {
            val state = current()
            if (!state.runtimePermissionGranted) return SinkResult.PermissionMissing
            if (!state.notificationsEnabledBySystem || !state.channelEnabled) return SinkResult.Disabled
            return try {
                platform.createChannel(
                    CHANNEL_ID,
                    R.string.notification_channel_app_messages,
                    R.string.notification_channel_app_messages_summary,
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                val resolved = textResolver.resolve(notice)
                platform.notify(
                    CHANNEL_ID,
                    notificationId(notice.eventId),
                    resolved.title,
                    resolved.text,
                    R.mipmap.launcher_logo,
                )
                SinkResult.Delivered
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SinkResult.Failed("notice.system_delivery_failed")
            }
        }

        internal companion object {
            const val CHANNEL_ID = "passly_app_messages"
            const val FIRST_NOTIFICATION_ID = 42_000
            const val ID_RANGE = 10_000

            fun notificationId(eventId: String): Int = FIRST_NOTIFICATION_ID + (eventId.hashCode() and Int.MAX_VALUE) % ID_RANGE
        }
    }
