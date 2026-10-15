package com.aozijx.passly.core.platform.packageinfo

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppIconLoader @Inject constructor(
    @ApplicationContext context: Context,
    private val logger: Logger,
) {
    private val packageManager = context.packageManager
    private val cache = LruCache<String, Bitmap>(60)
    private val targetSize = (48 * context.resources.displayMetrics.density).toInt()

    suspend fun loadIcon(packageName: String): Bitmap? = logger.inSpan(Operations.PackageIconLoad) {
        cache[packageName]?.let { return@inSpan it }
        try {
            convert(packageManager.getApplicationIcon(packageName))?.also { cache.put(packageName, it) }
        } catch (error: PackageManager.NameNotFoundException) {
            logger.log(LogStatements.Application.packageIconNotFound(error))
            null
        }
    }

    private fun convert(drawable: Drawable): Bitmap? = try {
        if (drawable is BitmapDrawable && drawable.bitmap.width == targetSize && drawable.bitmap.height == targetSize) {
            drawable.bitmap
        } else drawable.toBitmap(targetSize, targetSize)
    } catch (error: Exception) {
        logger.log(LogStatements.Application.packageIconDecodeFailed(error))
        null
    }
}
