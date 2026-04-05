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
    private val groupKey = "com.aozijx.passly.NOTIFICATION_GROUP"

    // 摘要通知的固定 ID
    private val summaryId = 0

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 建议不要在 init 中删除渠道，除非有特殊版本迁移需求
        // notificationManager.deleteNotificationChannel(channelId)
        
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
        val summaryNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.launcher_logo)
            .setContentTitle("收到多条通知")
            .setContentText("点击展开查看详情")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) 
            .build()

        notificationManager.notify(uniqueId, notification)
        notificationManager.notify(summaryId, summaryNotification)
    }

    private fun createPendingIntent(): PendingIntent {
        // 安全修复：显式指定包名和目标类，彻底消除隐式 Intent 风险
        val intent = Intent(context, FullActivity::class.java).apply {
            `package` = context.packageName // 明确指定包名
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            context,
            uniqueRequestCode(), // 使用唯一的 requestCode 防止 Intent 重复
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun uniqueRequestCode(): Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

    /**
     * 取消所有通知（包括堆叠的组）
     */
    fun cancelNotification() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
