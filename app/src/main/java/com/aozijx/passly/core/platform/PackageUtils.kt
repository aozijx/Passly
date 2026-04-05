package com.aozijx.passly.core.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.createBitmap
import com.aozijx.passly.core.logging.Logcat

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
     */
    fun getAllInstalledApps(context: Context, includeSystem: Boolean = false): List<AppWithSdk> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)

        return packages.mapNotNull { pkg ->
            pkg.toAppWithSdk(context, includeSystem)
        }
    }

    /**
     * 将 PackageInfo 转换为 AppWithSdk
     */
    fun PackageInfo.toAppWithSdk(context: Context, includeSystem: Boolean = true): AppWithSdk? {
        val appInfo = applicationInfo ?: return null
        val pm = context.packageManager
        
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        if (!includeSystem && isSystem) return null

        return AppWithSdk(
            packageName = packageName,
            appName = appInfo.loadLabel(pm).toString(),
            targetSdk = appInfo.targetSdkVersion,
            versionName = versionName ?: "N/A",
            architecture = getAppArchitecture(appInfo)
        )
    }

    private fun getAppArchitecture(appInfo: ApplicationInfo): String {
        return try {
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
                appInfo.nativeLibraryDir.contains("arm64") -> "arm64-v8a"
                appInfo.nativeLibraryDir.contains("arm") -> "armeabi-v7a"
                else -> "32-bit"
            }
        } catch (e: Exception) {
            Logcat.d("PackageUtils", "获取架构跳过: ${appInfo.packageName}")
            "Unknown"
        }
    }

    /**
     * 获取应用图标的 Drawable
     * 优化点：对未安装应用使用调试日志而非错误日志，避免日志污染
     */
    fun getAppIconDrawable(context: Context, packageName: String): Drawable? {
        return try {
            if (packageName.isBlank()) return null
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appInfo.loadIcon(pm)
        } catch (e: PackageManager.NameNotFoundException) {
            // 预期内的异常，使用静默处理或调试级别日志
            Logcat.d("PackageUtils", "应用未安装，跳过图标加载: $packageName")
            null
        } catch (e: Exception) {
            Logcat.w("PackageUtils", "获取图标未知异常: $packageName")
            null
        }
    }

    /**
     * 一次性获取应用名称和图标
     */
    fun getAppLabelAndIcon(context: Context, packageName: String): Pair<String, Drawable>? {
        return try {
            if (packageName.isBlank()) return null
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = appInfo.loadLabel(pm).toString()
            val icon = appInfo.loadIcon(pm)
            label to icon
        } catch (e: PackageManager.NameNotFoundException) {
            Logcat.d("PackageUtils", "应用未安装，跳过信息获取: $packageName")
            null
        } catch (e: Exception) {
            Logcat.w("PackageUtils", "获取应用信息异常: $packageName")
            null
        }
    }

    /**
     * 将 Drawable 转换为 Bitmap
     */
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmapWidth = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
        val bitmapHeight = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
        val bitmap = createBitmap(bitmapWidth, bitmapHeight)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * 将 Drawable 转换为 Compose 的 ImageBitmap
     */
    fun Drawable.toImageBitmap(): ImageBitmap {
        return drawableToBitmap(this).asImageBitmap()
    }
}

/**
 * Compose 专用：记住并加载应用图标的 Painter
 */
@Composable
fun rememberAppIconPainter(
    packageName: String
): Painter? {
    val context = LocalContext.current
    val appIconBitmap = remember(packageName) {
        try {
            val drawable = PackageUtils.getAppIconDrawable(context, packageName)
            if (drawable != null) {
                with(PackageUtils) { drawable.toImageBitmap() }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    return if (appIconBitmap != null) {
        BitmapPainter(appIconBitmap)
    } else {
        null
    }
}



