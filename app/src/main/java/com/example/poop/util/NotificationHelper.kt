package com.example.poop.util

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
import com.example.poop.MainActivity
import com.example.poop.R
import com.example.poop.data.Preference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class NotificationHelper(private val context: Context) {
    // 实例化 Preference
    private val preference = Preference(context)

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
        // 关键点：检查设置中的通知开关状态
        // 因为这是同步触发的操作，我们使用 runBlocking 获取 Flow 的当前值
        val isEnabled = runBlocking { preference.isNotificationsEnabled.first() }
        if (!isEnabled) return

        val notificationManager = NotificationManagerCompat.from(context)

        // 1. 使用当前时间戳作为唯一 ID，确保新通知不会覆盖旧通知，从而形成堆叠
        val uniqueId = System.currentTimeMillis().toInt()

        // 2. 构建子通知
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.app_icon_main)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setGroup(groupKey) // 关键：设置分组 Key
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(createPendingIntent())
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        // 3. 构建并发送摘要通知（Summary）
        // 摘要通知是堆叠的外壳，必须设置 setGroupSummary(true)
        val summaryNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.app_icon_main)
            .setContentTitle("收到多条通知") // 堆叠时的总标题
            .setContentText("点击展开查看详情")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(groupKey)
            .setGroupSummary(true) // 关键：标记为组摘要
            .setAutoCancel(true)
            .build()

        // 先发送子通知，再发送/更新摘要通知
        notificationManager.notify(uniqueId, notification)
        notificationManager.notify(summaryId, summaryNotification)
    }

    private fun createPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
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
