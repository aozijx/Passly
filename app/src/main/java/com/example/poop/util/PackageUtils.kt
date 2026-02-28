package com.example.poop.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class AppWithSdk(
    val packageName: String,
    val appName: String,
    val targetSdk: Int,
    val versionName: String,
    val architecture: String
)

object PackageUtils {

    /**
     * 获取所有已安装的应用及其 SDK 和架构信息
     * 注意：需要清单文件中声明 QUERY_ALL_PACKAGES 权限
     */
    fun getAllInstalledApps(context: Context): List<AppWithSdk> {
        val pm = context.packageManager
        // 获取所有已安装的包
        val packages = pm.getInstalledPackages(0)

        return packages.mapNotNull { pkg ->
            val appInfo = pkg.applicationInfo
            // 过滤掉系统应用（可选，根据需求保留或移除）
            if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                val arch = getAppArchitecture(appInfo)

                AppWithSdk(
                    packageName = pkg.packageName,
                    appName = appInfo.loadLabel(pm).toString(),
                    targetSdk = appInfo.targetSdkVersion,
                    versionName = pkg.versionName ?: "N/A",
                    architecture = arch
                )
            } else null
        }
    }

    /**
     * 尝试获取应用的 CPU 架构 (ABI)
     */
    private fun getAppArchitecture(appInfo: ApplicationInfo): String {
        return try {
            // 通过反射获取 primaryCpuAbi 字段
            val primaryCpuAbi = ApplicationInfo::class.java.getField("primaryCpuAbi")
                .get(appInfo) as? String

            when {
                primaryCpuAbi != null -> {
                    when {
                        primaryCpuAbi.contains("arm64-v8a") -> "arm64-v8a"
                        primaryCpuAbi.contains("armeabi-v7a") -> "armeabi-v7a"
                        primaryCpuAbi.contains("x86_64") -> "x86_64"
                        primaryCpuAbi.contains("x86") -> "x86"
                        primaryCpuAbi.contains("64") -> "64-bit"
                        else -> primaryCpuAbi
                    }
                }
                // 兜底方案：检查原生库目录路径
                appInfo.nativeLibraryDir.contains("arm64") -> "arm64-v8a"
                appInfo.nativeLibraryDir.contains("arm") -> "armeabi-v7a"
                else -> "32-bit"
            }
        } catch (e: Exception) {
            Logcat.e("PackageUtils", "获取架构失败: ${appInfo.packageName}", e)
            "Unknown"
        }
    }

    /**
     * 检查是否有权查询所有包
     */
    fun canQueryAllPackages(context: Context): Boolean {
        // 由于 minSdk >= 31，不再需要检查 SDK_INT
        return context.packageManager.checkPermission(
            android.Manifest.permission.QUERY_ALL_PACKAGES,
            context.packageName
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 根据应用名称（模糊匹配）获取包名
     */
    fun getPackageNameByAppName(context: Context, appName: String): String? {
        val pm: PackageManager = context.packageManager
        val installedApps: List<ApplicationInfo> =
            pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.contains(appName, ignoreCase = true)) {
                return app.packageName
            }
        }
        return null
    }

    /**
     * 检查某个应用是否安装
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Logcat.e("PackageUtils", "App not found: $packageName", e)
            false
        }
    }
}
