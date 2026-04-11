package com.aozijx.passly.core.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.aozijx.passly.core.logging.Logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用元数据模型
 */
data class AppMetadata(
    val packageName: String,
    val appName: String,
    val targetSdk: Int,
    val versionName: String,
    val architecture: String
)

object PackageUtils {
    private val iconCache = LruCache<String, ImageBitmap>(50)

    /**
     * 异步获取应用元数据
     * 这里的 Result 处理了应用未安装的情况，不再需要手动判断
     */
    suspend fun getAppMetadata(context: Context, packageName: String): AppMetadata? =
        withContext(Dispatchers.IO) {
            if (packageName.isBlank()) return@withContext null
            runCatching {
                val pm = context.packageManager
                val pkgInfo = pm.getPackageInfo(packageName, 0)
                val appInfo = pkgInfo.applicationInfo ?: return@runCatching null

                AppMetadata(
                    packageName = packageName,
                    appName = appInfo.loadLabel(pm).toString(),
                    targetSdk = appInfo.targetSdkVersion,
                    versionName = pkgInfo.versionName ?: "N/A",
                    architecture = getAppArchitecture(appInfo)
                )
            }.getOrNull()
        }

    /**
     * 异步加载图标并缓存
     */
    suspend fun loadIcon(context: Context, packageName: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            if (packageName.isBlank()) return@withContext null
            iconCache[packageName]?.let { return@withContext it }

            runCatching {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val bitmap = appInfo.loadIcon(pm).toBitmap().asImageBitmap()
                iconCache.put(packageName, bitmap)
                bitmap
            }.onFailure {
                Logcat.d("PackageUtils", "加载图标失败: $packageName")
            }.getOrNull()
        }

    private fun getAppArchitecture(appInfo: ApplicationInfo): String {
        return try {
            val primaryCpuAbi =
                ApplicationInfo::class.java.getField("primaryCpuAbi").get(appInfo) as? String

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
            Logcat.d("PackageUtils", "获取应用架构失败: ${e.message}")
            "Unknown"
        }
    }
}

/**
 * Compose 专用图标加载 Hook
 * 返回 null 表示正在加载或应用不存在。
 */
@Composable
fun rememberAppIcon(packageName: String?): Painter? {
    if (packageName.isNullOrBlank()) return null
    val context = LocalContext.current.applicationContext

    // 使用 produceState 自动处理协程加载，packageName 变化时自动重载
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = PackageUtils.loadIcon(context, packageName)
    }

    return remember(bitmap) {
        bitmap?.let { BitmapPainter(it) }
    }
}