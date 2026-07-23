package com.aozijx.passly.feature.settings.datamanagement

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.media.FaviconUtils
import com.aozijx.passly.core.media.ImageResolver.isRemoteIconPath
import com.aozijx.passly.domain.model.entry.EntryChanges
import com.aozijx.passly.domain.repository.entry.EntryCommandRepository
import com.aozijx.passly.domain.usecase.settings.PortableSettingsUseCases
import com.aozijx.passly.domain.usecase.vault.VaultQueryUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

internal data class BackupImportIconSyncResult(
    val total: Int,
    val success: Int,
    val failed: Int,
    val skippedByNoNetwork: Boolean = false,
    val failedEntryIds: List<String> = emptyList()
)

internal class BackupImportIconSyncSupport(
    private val entryCommandRepository: EntryCommandRepository,
    private val vaultQueryUseCases: VaultQueryUseCases,
    private val portableSettingsUseCases: PortableSettingsUseCases
) {

    private companion object {
        private const val TAG = "BackupIconSync"
    }

    suspend fun syncRemoteIcons(
        context: Context,
        onProgress: ((processed: Int, total: Int, success: Int, failed: Int) -> Unit)?
    ): BackupImportIconSyncResult = withContext(Dispatchers.IO) {
        val whitelist = portableSettingsUseCases.faviconDownloadWhitelist.first()
        val appContext = context.applicationContext
        if (!hasActiveNetwork(appContext)) {
            AppLog.w(TAG, "Skip icon sync: no active network")
            return@withContext BackupImportIconSyncResult(
                total = 0,
                success = 0,
                failed = 0,
                skippedByNoNetwork = true
            )
        }

        val targets = vaultQueryUseCases.getEntriesForIconResync()

        if (targets.isEmpty()) {
            onProgress?.invoke(0, 0, 0, 0)
            return@withContext BackupImportIconSyncResult(total = 0, success = 0, failed = 0)
        }

        var successCount = 0
        var failedCount = 0
        val failedIds = mutableListOf<String>()
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

            val outcome = FaviconUtils.downloadAndSaveFavicon(source, appContext, whitelist)
            if (outcome.result == FaviconUtils.DownloadResult.SUCCESS && !outcome.filePath.isNullOrBlank()) {
                val updateResult = entryCommandRepository.updateEntry(
                    entry.id, entry.entryVersion,
                    EntryChanges(summary = entry.summary.copy(icon = outcome.filePath))
                )
                if (updateResult.isSuccess) {
                    successCount++
                } else {
                    failedCount++
                    failedIds += entry.id
                    AppLog.w(TAG, "Icon sync update failed: entryId=${entry.id}")
                }
            } else {
                failedCount++
                failedIds += entry.id
                AppLog.w(
                    TAG,
                    "Icon sync failed: entryId=${entry.id}, source=$source, result=${outcome.result}"
                )
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

        AppLog.i(
            TAG,
            "Icon sync done: total=${result.total}, success=${result.success}, failed=${result.failed}"
        )
        result
    }

    private fun hasActiveNetwork(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
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
