package com.aozijx.passly.features.settings.internal

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.media.FaviconUtils
import com.aozijx.passly.core.media.isRemoteIconPath
import com.aozijx.passly.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal data class BackupImportIconSyncResult(
    val total: Int,
    val success: Int,
    val failed: Int,
    val skippedByNoNetwork: Boolean = false,
    val failedEntryIds: List<Int> = emptyList()
)

internal class BackupImportIconSyncSupport {

    private companion object {
        private const val TAG = "BackupIconSync"
    }

    suspend fun syncRemoteIcons(
        context: Context,
        onProgress: ((processed: Int, total: Int, success: Int, failed: Int) -> Unit)?
    ): BackupImportIconSyncResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        if (!hasActiveNetwork(appContext)) {
            Logcat.w(TAG, "Skip icon sync: no active network")
            return@withContext BackupImportIconSyncResult(
                total = 0,
                success = 0,
                failed = 0,
                skippedByNoNetwork = true
            )
        }

        val dao = AppDatabase.getDatabase(appContext).vaultDao()
        val targets = dao.getEntriesForIconResync()

        if (targets.isEmpty()) {
            onProgress?.invoke(0, 0, 0, 0)
            return@withContext BackupImportIconSyncResult(total = 0, success = 0, failed = 0)
        }

        var successCount = 0
        var failedCount = 0
        val failedIds = mutableListOf<Int>()
        var processedCount = 0

        onProgress?.invoke(0, targets.size, successCount, failedCount)

        targets.forEach { entry ->
            val source = resolveDownloadSource(entry.associatedDomain, entry.iconCustomPath)
            if (source == null) {
                failedCount++
                failedIds += entry.id
                processedCount++
                onProgress?.invoke(processedCount, targets.size, successCount, failedCount)
                return@forEach
            }

            val outcome = FaviconUtils.downloadAndSaveFavicon(source, appContext)
            if (outcome.result == FaviconUtils.DownloadResult.SUCCESS && !outcome.filePath.isNullOrBlank()) {
                dao.update(
                    entry.copy(
                        iconName = null,
                        iconCustomPath = outcome.filePath,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                successCount++
            } else {
                failedCount++
                failedIds += entry.id
                Logcat.w(TAG, "Icon sync failed: entryId=${entry.id}, source=$source, result=${outcome.result}")
            }
            processedCount++
            onProgress?.invoke(processedCount, targets.size, successCount, failedCount)
        }

        val result = BackupImportIconSyncResult(
            total = targets.size,
            success = successCount,
            failed = failedCount,
            failedEntryIds = failedIds
        )

        Logcat.i(TAG, "Icon sync done: total=${result.total}, success=${result.success}, failed=${result.failed}")
        result
    }

    private fun hasActiveNetwork(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun resolveDownloadSource(domain: String?, currentPath: String?): String? {
        val trimmedPath = currentPath?.trim().orEmpty()
        val trimmedDomain = domain?.trim().orEmpty()
        val isRemotePath = isRemoteIconPath(trimmedPath)

        if (isRemotePath) return trimmedPath

        if (trimmedPath.isNotEmpty()) {
            val localFileExists = runCatching { File(trimmedPath).exists() }.getOrDefault(false)
            if (localFileExists) return null
        }

        return trimmedDomain.takeIf { it.isNotBlank() }
    }
}
