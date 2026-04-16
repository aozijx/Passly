package com.aozijx.passly.features.backup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aozijx.passly.R
import com.aozijx.passly.features.settings.internal.BackupImportIconSyncSupport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BackupImportIconSyncForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val iconSyncSupport = BackupImportIconSyncSupport()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        val notification = buildNotification(
            content = "正在同步备份图标...",
            ongoing = true,
            processed = 0,
            total = 0
        )
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        serviceScope.launch {
            val result = iconSyncSupport.syncRemoteIcons(this@BackupImportIconSyncForegroundService) { processed, total, success, failed ->
                val content = if (total > 0) {
                    "正在同步图标：$processed/$total（成功 $success，失败 $failed）"
                } else {
                    "正在同步备份图标..."
                }
                notificationManager().notify(
                    NOTIFICATION_ID,
                    buildNotification(
                        content = content,
                        ongoing = true,
                        processed = processed,
                        total = total
                    )
                )
            }
            val content = when {
                result.skippedByNoNetwork -> "未联网，图标同步已跳过"
                result.total == 0 -> "没有需要下载的远程图标"
                else -> "图标同步完成：${result.success}/${result.total}"
            }

            notificationManager().notify(
                NOTIFICATION_ID,
                buildNotification(
                    content = content,
                    ongoing = false,
                    processed = result.total,
                    total = result.total
                )
            )
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(
        content: String,
        ongoing: Boolean,
        processed: Int,
        total: Int
    ): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.launcher_logo)
            .setContentTitle("Passly 备份图标同步")
            .setContentText(content)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (ongoing) {
            if (total > 0) {
                builder.setProgress(total, processed.coerceIn(0, total), false)
            } else {
                builder.setProgress(0, 0, true)
            }
        } else {
            builder.setProgress(0, 0, false)
        }

        return builder.build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "备份图标同步",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "显示备份导入后图标同步进度"
            setShowBadge(false)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager {
        return getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        private const val CHANNEL_ID = "backup_icon_sync_v2"
        private const val NOTIFICATION_ID = 41002

        fun start(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, BackupImportIconSyncForegroundService::class.java)
            appContext.startForegroundService(intent)
        }
    }
}