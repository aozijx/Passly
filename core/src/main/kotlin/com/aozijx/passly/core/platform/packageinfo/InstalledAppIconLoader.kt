package com.aozijx.passly.core.platform.packageinfo

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.report
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppIconLoader @Inject constructor(
    @ApplicationContext context: Context,
    private val telemetry: TelemetryReporter,
) {
    private val packageManager = context.packageManager
    private val cache = LruCache<String, Bitmap>(60)
    private val targetSize = (48 * context.resources.displayMetrics.density).toInt()

    fun loadIcon(packageName: String): Bitmap? {
        cache[packageName]?.let { return it }
        return try {
            convert(packageManager.getApplicationIcon(packageName))?.also { cache.put(packageName, it) }
        } catch (error: PackageManager.NameNotFoundException) {
            report(EventLevel.WARN, "platform.package_icon_not_found", error)
            null
        }
    }

    private fun convert(drawable: Drawable): Bitmap? = try {
        if (drawable is BitmapDrawable && drawable.bitmap.width == targetSize && drawable.bitmap.height == targetSize) {
            drawable.bitmap
        } else drawable.toBitmap(targetSize, targetSize)
    } catch (error: Exception) {
        report(EventLevel.ERROR, "platform.package_icon_decode_failed", error)
        null
    }

    private fun report(level: EventLevel, name: String, error: Throwable) =
        telemetry.report(level, EventCategory.APPLICATION, name, error)
}
