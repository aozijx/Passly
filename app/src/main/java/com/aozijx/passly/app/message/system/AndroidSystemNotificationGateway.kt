package com.aozijx.passly.app.message.system

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aozijx.passly.R
import com.aozijx.passly.core.permission.contract.PermissionStatusReader
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.app.message.model.AppNotice
import com.aozijx.passly.app.message.contract.NoticeTextResolver
import com.aozijx.passly.app.message.contract.SinkResult
import com.aozijx.passly.app.message.contract.SystemNotificationGateway
import com.aozijx.passly.app.message.contract.SystemNotificationState
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSystemNotificationGateway @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionStatusReader: PermissionStatusReader,
    private val textResolver: NoticeTextResolver
) : SystemNotificationGateway, SystemNotificationStateProvider {
    private val notificationManager = NotificationManagerCompat.from(context)

    override fun current(): SystemNotificationState {
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL_ID)
        return SystemNotificationState(
            userSettingEnabled = true,
            runtimePermissionGranted = permissionStatusReader.status(
                RuntimePermission.POST_NOTIFICATIONS
            ) != PermissionStatus.DENIED,
            notificationsEnabledBySystem = notificationManager.areNotificationsEnabled(),
            channelEnabled = channel == null ||
                channel.importance != NotificationManager.IMPORTANCE_NONE
        )
    }

    @SuppressLint("MissingPermission")
    override suspend fun deliver(notice: AppNotice): SinkResult {
        val state = current()
        if (!state.runtimePermissionGranted) return SinkResult.PermissionMissing
        if (!state.notificationsEnabledBySystem || !state.channelEnabled) return SinkResult.Disabled
        return runCatching {
            ensureChannel()
            val resolved = textResolver.resolve(notice)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.launcher_logo)
                .setContentTitle(resolved.title)
                .setContentText(resolved.text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(resolved.text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(
                FIRST_NOTIFICATION_ID +
                    (notice.eventId.hashCode() and Int.MAX_VALUE) % ID_RANGE,
                notification
            )
            SinkResult.Delivered
        }.getOrElse {
            SinkResult.Failed("notice.system_delivery_failed")
        }
    }

    private fun ensureChannel() {
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_app_messages),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(
                        R.string.notification_channel_app_messages_summary
                    )
                }
            )
    }

    private companion object {
        const val CHANNEL_ID = "passly_app_messages"
        const val FIRST_NOTIFICATION_ID = 42_000
        const val ID_RANGE = 10_000
    }
}
