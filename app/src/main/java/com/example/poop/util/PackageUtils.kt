package com.example.poop.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object PackageUtils {

    /**
     * 根据应用名称（模糊匹配）获取包名
     */
    fun getPackageNameByAppName(context: Context, appName: String): String? {
        val pm: PackageManager = context.packageManager
        val installedApps: List<ApplicationInfo> = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.contains(appName, ignoreCase = true)) {
                return app.packageName
            }
        }
        return null
    }

    /**
     * 常用包名常量（备选方案）
     */
    const val WECHAT = "com.tencent.mm"
    const val QQ = "com.tencent.mobileqq"
    const val DOUYIN = "com.ss.android.ugc.aweme" // 抖音主版
    const val DOUYIN_LITE = "com.ss.android.ugc.aweme.lite" // 抖音极速版

    /**
     * 检查某个应用是否安装
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
