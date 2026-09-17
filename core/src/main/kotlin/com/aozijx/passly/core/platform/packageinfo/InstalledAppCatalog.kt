package com.aozijx.passly.core.platform.packageinfo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.LruCache
import com.aozijx.passly.domain.autofill.port.ApplicationLabelResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledAppMetadata(val label: String, val packageName: String)

@Singleton
class InstalledAppCatalog @Inject constructor(
    @ApplicationContext context: Context,
) : ApplicationLabelResolver, InstalledAppDirectory {
    private val packageManager: PackageManager = context.packageManager
    private val cache = LruCache<String, InstalledAppMetadata>(60)

    override suspend fun metadataFor(packageName: String): InstalledAppMetadata? =
        withContext(Dispatchers.IO) { metadataForBlocking(packageName) }

    private fun metadataForBlocking(packageName: String): InstalledAppMetadata? {
        cache[packageName]?.let { return it }
        val label = resolveLabel(packageName) ?: return null
        return InstalledAppMetadata(label, packageName).also { cache.put(packageName, it) }
    }

    override fun labelFor(packageName: String): String? = metadataForBlocking(packageName)?.label

    @SuppressLint("QueryPermissionsNeeded")
    private fun resolveLabel(packageName: String): String? {
        runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull()?.let { info ->
            packageManager.getApplicationLabel(info).toString().trim()
                .takeIf { it.isNotBlank() && !it.equals(packageName, true) }?.let { return it }
        }
        return packageManager.queryIntentActivities(launcherIntent(), 0).asSequence()
            .filter { it.activityInfo?.packageName == packageName }
            .firstNotNullOfOrNull { it.loadLabel(packageManager).toString().trim().takeIf(String::isNotBlank) }
            ?.takeUnless { it.equals(packageName, true) }
    }

    @SuppressLint("QueryPermissionsNeeded")
    override suspend fun launchableApps(): List<InstalledAppMetadata> = withContext(Dispatchers.IO) {
        packageManager.queryIntentActivities(launcherIntent(), 0).asSequence()
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                InstalledAppMetadata(info.loadLabel(packageManager).toString().takeIf(String::isNotBlank) ?: packageName, packageName)
            }
            .distinctBy(InstalledAppMetadata::packageName)
            .sortedWith(compareBy<InstalledAppMetadata> { it.label.lowercase() }.thenBy { it.packageName })
            .toList().also { apps -> apps.forEach { cache.put(it.packageName, it) } }
    }

    private fun launcherIntent() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
}
