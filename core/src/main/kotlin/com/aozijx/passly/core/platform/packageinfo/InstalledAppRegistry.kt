package com.aozijx.passly.core.platform.packageinfo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppRegistry @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val telemetry: TelemetryReporter,
) {
    private val packageManager: PackageManager = context.packageManager

    private val iconCache = LruCache<String, Bitmap>(MAX_ICON_CACHE_SIZE)
    private val metadataCache = LruCache<String, AppMetadata>(MAX_METADATA_CACHE_SIZE)

    private val targetIconSize: Int by lazy {
        (DEFAULT_ICON_SIZE_DP * context.resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val MAX_ICON_CACHE_SIZE = 60
        private const val MAX_METADATA_CACHE_SIZE = 60
        private const val DEFAULT_ICON_SIZE_DP = 48
    }

    data class AppMetadata(
        val label: String,
        val packageName: String,
    )

    fun getAppMetadata(packageName: String): AppMetadata? {
        val cached = metadataCache[packageName]
        if (cached != null) return cached

        val label = resolveAppLabel(packageName) ?: return null
        val metadata = AppMetadata(label = label, packageName = packageName)
        metadataCache.put(packageName, metadata)
        return metadata
    }

    /**
     * Resolves the display label for a package with fallbacks and Android 11+ visibility awareness.
     *
     * 1. Check metadata cache.
     * 2. Use ApplicationInfo.
     * 3. Use Launcher Intent query.
     * 4. Return null if no descriptive label is found.
     */
    @SuppressLint("QueryPermissionsNeeded")
    private fun resolveAppLabel(packageName: String): String? {
        // 1. Check cache (usually warmed up by getLaunchableApps)
        metadataCache[packageName]?.label?.let { return it }

        // 2. ApplicationInfo check (handles non-launcher system components)
        runCatching {
            packageManager.getApplicationInfo(packageName, 0)
        }.getOrNull()?.let { appInfo ->
            packageManager.getApplicationLabel(appInfo).toString()
                .trim()
                .takeIf { it.isNotBlank() && !it.equals(packageName, ignoreCase = true) }
                ?.let { return it }
        }

        // 3. Launcher query fallback (most reliable for third-party apps)
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .filter { it.activityInfo?.packageName == packageName }
            .firstNotNullOfOrNull { resolveInfo ->
                resolveInfo.loadLabel(packageManager).toString()
                    .trim()
                    .takeIf { it.isNotBlank() }
            }
            ?.takeUnless { it.equals(packageName, ignoreCase = true) }
    }

    /**
     * Returns a list of apps with launcher activities.
     */
    @SuppressLint("QueryPermissionsNeeded")
    fun getLaunchableApps(): List<AppMetadata> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val result = packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager).toString()
                    .takeIf { it.isNotBlank() } ?: packageName
                AppMetadata(label = label, packageName = packageName)
            }
            .distinctBy { it.packageName }
            .sortedWith(
                compareBy<AppMetadata> { it.label.lowercase() }
                    .thenBy { it.packageName }
            )
            .toList()

        // Warm up cache
        result.forEach { metadataCache.put(it.packageName, it) }
        return result
    }

    fun loadIcon(packageName: String): Bitmap? {
        val cachedBitmap = iconCache[packageName]
        if (cachedBitmap != null) return cachedBitmap

        return try {
            val appIcon = packageManager.getApplicationIcon(packageName)
            val bitmap = convertDrawableToBitmap(appIcon)
            if (bitmap != null) {
                iconCache.put(packageName, bitmap)
                bitmap
            } else {
                null
            }
        } catch (e: PackageManager.NameNotFoundException) {
            report(EventLevel.WARN, "platform.package_icon_not_found", e)
            null
        }
    }

    private fun convertDrawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            if (
                (drawable is BitmapDrawable) &&
                (drawable.bitmap.width == targetIconSize) &&
                (drawable.bitmap.height == targetIconSize)
            ) {
                drawable.bitmap
            } else {
                drawable.toBitmap(targetIconSize, targetIconSize)
            }
        } catch (e: Exception) {
            report(EventLevel.ERROR, "platform.package_icon_decode_failed", e)
            null
        }
    }

    private fun report(level: EventLevel, name: String, throwable: Throwable? = null) {
        telemetry.report(level, EventCategory.APPLICATION, name, throwable)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface InstalledAppRegistryProvider {
    fun getInstalledAppRegistry(): InstalledAppRegistry
}
