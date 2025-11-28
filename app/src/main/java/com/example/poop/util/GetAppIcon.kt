package com.example.poop.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.painter.BitmapPainter
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log

// 添加日志标签
private const val TAG = "AppIconUtil"

// Drawable 转 ImageBitmap 转换
private fun Drawable.toImageBitmap(): ImageBitmap {
    return try {
        val bitmapWidth = if (intrinsicWidth > 0) intrinsicWidth else 1
        val bitmapHeight = if (intrinsicHeight > 0) intrinsicHeight else 1

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)

        bitmap.asImageBitmap()
    } catch (e: Exception) {
        Log.e(TAG, "Error converting drawable to ImageBitmap", e)
        // 创建默认的 1x1 像素位图作为备用
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}

// 应用图标获取
private fun getAppIconDrawable(context: Context, packageName: String): Drawable? {
    return try {
        Log.d(TAG, "Attempting to get icon for package: $packageName")

        val packageManager = context.packageManager

        // 方法1: 直接获取应用图标
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        val drawable = appInfo.loadIcon(packageManager)

        Log.d(TAG, "Successfully loaded icon for: $packageName")
        drawable
    } catch (e: PackageManager.NameNotFoundException) {
        Log.w(TAG, "Package not found: $packageName")
        null
    } catch (e: SecurityException) {
        Log.w(TAG, "Security exception for package: $packageName", e)
        null
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected error getting icon for: $packageName", e)
        null
    }
}

// 主要的 Composable 函数
@Composable
fun rememberAppIconPainter(
    packageName: String,
    defaultIconResId: Int
): Painter {
    val context = LocalContext.current

    // 用 remember 缓存结果
    val appIconBitmap = remember(packageName) {
        val drawable = getAppIconDrawable(context, packageName)
        if (drawable != null) {
            Log.d(TAG, "Successfully converted drawable to bitmap for: $packageName")
            drawable.toImageBitmap()
        } else {
            Log.w(TAG, "Failed to get drawable for: $packageName, using default icon")
            null
        }
    }

    return if (appIconBitmap != null) {
        BitmapPainter(appIconBitmap)
    } else {
        // 只有在完全失败时才使用默认图标
        androidx.compose.ui.res.painterResource(id = defaultIconResId)
    }
}

// 调试函数：检查包是否存在
@Composable
fun debugPackageInfo(packageName: String) {
    val context = LocalContext.current
    val packageInfo = remember(packageName) {
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            "Package exists: $packageName"
        } catch (e: Exception) {
            "Package NOT found: $packageName - ${e.message}"
        }
    }

    Log.d(TAG, packageInfo)
}