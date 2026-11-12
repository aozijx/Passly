package com.aozijx.passly.app.message.system

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aozijx.passly.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSystemNotificationPlatform
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : SystemNotificationPlatform {
        private val notificationManager = NotificationManagerCompat.from(context)

        override fun areNotificationsEnabled(): Boolean = notificationManager.areNotificationsEnabled()

        override fun channelImportance(channelId: String): Int? =
            context
                .getSystemService(NotificationManager::class.java)
                .getNotificationChannel(channelId)
                ?.importance

        override fun createChannel(
            channelId: String,
            nameResourceId: Int,
            descriptionResourceId: Int,
            importance: Int,
        ) {
            context
                .getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        context.getString(nameResourceId),
                        importance,
                    ).apply {
                        description = context.getString(descriptionResourceId)
                    },
                )
        }

        @SuppressLint("MissingPermission")
        override fun notify(
            channelId: String,
            notificationId: Int,
            title: String,
            text: String,
            smallIconResourceId: Int,
        ) {
            val notification =
                NotificationCompat
                    .Builder(context, channelId)
                    .setSmallIcon(smallIconResourceId)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
            notificationManager.notify(notificationId, notification)
        }
    }
