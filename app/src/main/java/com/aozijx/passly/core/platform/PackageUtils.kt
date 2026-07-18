package com.aozijx.passly.core.platform

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.aozijx.passly.core.diagnostics.AppLog
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageUtils @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    private val iconCache = LruCache<String, Bitmap>(MAX_ICON_CACHE_SIZE)
    private val metadataCache =
        LruCache<String, AppMetadata>(MAX_METADATA_CACHE_SIZE)

    companion object {
        private const val TAG = "PackageUtils"
        private const val MAX_ICON_CACHE_SIZE = 60
        private const val MAX_METADATA_CACHE_SIZE = 60
        private const val DEFAULT_ICON_SIZE_DP = 48
    }

    data class AppMetadata(
        val appName: String,
        val packageName: String
    )

    fun getAppMetadata(packageName: String): AppMetadata? {
        val cached = metadataCache.get(packageName)
        if (cached != null) return cached

        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val metadata = AppMetadata(appName = appName, packageName = packageName)
            metadataCache.put(packageName, metadata)
            metadata
        } catch (e: PackageManager.NameNotFoundException) {
            AppLog.w(TAG, "App not found for package: $packageName", e)
            null
        }
    }

    fun loadIcon(packageName: String): ImageBitmap? {
        val cachedBitmap = iconCache.get(packageName)
        if (cachedBitmap != null) {
            return cachedBitmap.asImageBitmap()
        }

        return try {
            val appIcon = packageManager.getApplicationIcon(packageName)
            val bitmap = convertDrawableToBitmap(appIcon)
            if (bitmap != null) {
                iconCache.put(packageName, bitmap)
                bitmap.asImageBitmap()
            } else {
                null
            }
        } catch (e: PackageManager.NameNotFoundException) {
            AppLog.w(TAG, "Icon not found for package: $packageName", e)
            null
        }
    }

    private fun convertDrawableToBitmap(drawable: Drawable): Bitmap? {
        return when (drawable) {
            is BitmapDrawable -> {
                val bitmap = drawable.bitmap
                val density = context.resources.displayMetrics.density
                val targetSize = (DEFAULT_ICON_SIZE_DP * density).toInt()
                if (bitmap.width == targetSize && bitmap.height == targetSize) {
                    bitmap
                } else {
                    bitmap.scale(targetSize, targetSize)
                }
            }

            else -> {
                val density = context.resources.displayMetrics.density
                val targetSize = (DEFAULT_ICON_SIZE_DP * density).toInt()
                try {
                    drawable.toBitmap(targetSize, targetSize)
                } catch (e: Exception) {
                    AppLog.e(TAG, "Failed to convert drawable to bitmap", e)
                    null
                }
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PackageUtilsProvider {
    fun getPackageUtils(): PackageUtils
}