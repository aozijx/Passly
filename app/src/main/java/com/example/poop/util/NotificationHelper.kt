package com.example.poop.util

import android.Manifest
import android.app.Notification
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

class NotificationHelper(private val context: Context) {
    // 通知渠道ID（自定义）
    private val CHANNEL_ID = "STATUS_BAR_POPUP_CHANNEL"

    // 通知ID（用于区分不同通知）
    val NOTIFICATION_ID = 1001

    init {
        // 创建通知渠道（API 26+ 必须）
        createNotificationChannel()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 先删除旧渠道（避免华为缓存旧配置）
        notificationManager.deleteNotificationChannel(CHANNEL_ID)
        val channelName = "状态栏弹窗渠道"
        val channelDescription = "用于显示状态栏弹窗的通知渠道"
        // 重要性：HIGH 会在状态栏弹出提示，DEFAULT 仅显示图标
        val importance = NotificationManager.IMPORTANCE_HIGH
        // 获取系统默认的通知声音
        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) // 降级兜底

        val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
            description = channelDescription
            // 开启震动（可选）
            enableVibration(true)
            // 震动模式：等待 0ms，震动 1000ms，等待 1000ms，震动 1000ms
            vibrationPattern = longArrayOf(0, 1000, 1000, 1000)
            // 开启声音（可选）
            setSound(defaultSoundUri, null) // 如需声音，替换为实际铃声Uri
            // 设置渠道重要性（影响声音播放）
            setImportance(importance)
            // 锁屏显示（可选）
            setShowBadge(true)
        }

        // 注册渠道到系统
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 构建状态栏弹窗通知
     */
    fun buildStatusBarPopupNotification(): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.web_link) // 替换为你的图标
            .setContentTitle("状态栏弹窗标题").setContentText("这是Compose触发的状态栏弹窗内容")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE).setAutoCancel(true) // 点击后自动取消
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏可见
            // 可选：添加点击跳转（如打开Activity）
//             .setContentIntent(pendingIntent)
            // 可选：设置大文本样式
            .setStyle(
                NotificationCompat.BigTextStyle().bigText("这是扩展后的大文本内容，展示更多信息")
            ).build()
    }

    /**
     * 发送自定义内容通知
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendCustomNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.app_icon_main)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 高优先级（API 25-）
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true) // 点击后自动取消
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏可见
            .setContentIntent(
                PendingIntent.getActivity(
                context,
                0,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            ))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText("这是扩展后的大文本内容，展示更多信息")
            )
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /**
     * 取消通知
     */
    fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}