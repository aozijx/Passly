package com.example.poop.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.poop.model.AppOpenConfig

/**
 * 启动目标应用或功能的工具类
 */
object AppLauncherUtil {
    // 打开目标应用/功能（被UI组件调用）
    fun openTarget(config: AppOpenConfig, context: Context) {
        val intent: Intent? = when {
            config.intentBuilder != null -> config.intentBuilder.invoke(context)
            config.packageName != null -> context.packageManager.getLaunchIntentForPackage(config.packageName)
            else -> null
        }

        if (intent != null) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            Toast.makeText(context, config.errorTip, Toast.LENGTH_SHORT).show()
        }
    }

    // 检查应用是否已安装（可选工具方法）
    fun isAppInstalled(packageName: String, context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}