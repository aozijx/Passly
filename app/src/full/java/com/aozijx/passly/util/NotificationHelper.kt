package com.aozijx.passly.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aozijx.passly.FullActivity
import com.aozijx.passly.R
import com.aozijx.passly.data.AppPreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class NotificationHelper(private val context: Context) {
    // 实例化 Preference
    private val preference = AppPreference(context)

    // 通知渠道ID
    private val channelId = "STATUS_BAR_POPUP_CHANNEL"

    // 通知分组 Key（相同 Key 的通知会被堆叠在一起）
    private val groupKey = "com.example.poop.NOTIFICATION_GROUP"

    // 摘要通知的固定 ID
    private val summaryId = 0

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.deleteNotificationChannel(channelId)
        val channelName = "状态栏弹窗渠道"
        val channelDescription = "用于显示状态栏弹窗的通知渠道"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 1000, 1000)
            setSound(defaultSoundUri, null)
            setImportance(importance)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 发送支持堆叠的通知
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendCustomNotification(title: String, message: String) {
        val isEnabled = runBlocking { preference.isNotificationsEnabled.first() }
        if (!isEnabled) return

        val notificationManager = NotificationManagerCompat.from(context)
        val uniqueId = System.currentTimeMillis().toInt()
        val pendingIntent = createPendingIntent()

        // 1. 构建子通知
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.launcher_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setGroup(groupKey)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        // 2. 构建并发送摘要通知（Summary）
        // 修复：为摘要通知也添加显式的 PendingIntent
        val summaryNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.launcher_logo)
            .setContentTitle("收到多条通知")
            .setContentText("点击展开查看详情")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // 修复点：必须显式设置 Intent
            .build()

        notificationManager.notify(uniqueId, notification)
        notificationManager.notify(summaryId, summaryNotification)
    }

    private fun createPendingIntent(): PendingIntent {
        // 使用显式 Intent 指定目标 Activity，防止 Intent 被劫持修改
        val intent = Intent(context, FullActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 取消所有通知（包括堆叠的组）
     */
    fun cancelNotification() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
