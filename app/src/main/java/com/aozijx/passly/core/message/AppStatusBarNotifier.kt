package com.aozijx.passly.core.message

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aozijx.passly.R
import com.aozijx.passly.core.permission.AppPermission
import com.aozijx.passly.core.permission.AppPermissionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStatusBarNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionManager: AppPermissionManager
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val nextNotificationId = AtomicInteger(FIRST_NOTIFICATION_ID)

    @SuppressLint("MissingPermission")
    fun show(message: AppMessage): Boolean {
        if (message.presentation != AppMessagePresentation.STATUS_BAR) return false
        if (!canPostNotifications()) return false
        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.launcher_logo)
            .setContentTitle(message.title ?: context.getString(R.string.app_name))
            .setContentText(message.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(nextNotificationId.getAndIncrement(), notification)
        return true
    }

    private fun canPostNotifications(): Boolean {
        return permissionManager.snapshot(AppPermission.Notifications).isSatisfied &&
                notificationManager.areNotificationsEnabled()
    }

    private fun ensureChannel() {
        val systemManager = context.getSystemService(NotificationManager::class.java)
        systemManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_app_messages),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_app_messages_summary)
            }
        )
    }

    private companion object {
        const val CHANNEL_ID = "passly_app_messages"
        const val FIRST_NOTIFICATION_ID = 42_000
    }
}
