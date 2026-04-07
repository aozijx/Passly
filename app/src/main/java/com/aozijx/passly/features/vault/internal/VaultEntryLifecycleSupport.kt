package com.aozijx.passly.features.vault.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.icon.FaviconResult
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.domain.usecase.vault.VaultUseCases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class VaultEntryLifecycleSupport(
    private val vaultUseCases: VaultUseCases,
    private val entryFileSupport: VaultEntryFileSupport
) {
    suspend fun addEntry(entry: VaultEntry): Long {
        return vaultUseCases.insertEntry(entry)
    }

    suspend fun addEntryWithFavicon(entry: VaultEntry, domain: String) {
        val insertedId = addEntry(entry)
        if (domain.isBlank()) return

        val outcome = vaultUseCases.downloadFavicon(domain)
        if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
            val updatedEntry = entry.copy(
                id = insertedId.toInt(),
                iconName = null,
                iconCustomPath = outcome.filePath
            )
            vaultUseCases.updateEntry(updatedEntry)
        }
    }

    /**
     * 批量自动更新缺失的图标（针对 VaultSummary 列表）
     */
    fun autoUpdateMissingIcons(summaries: List<VaultSummary>, scope: CoroutineScope) {
        summaries.filter { !it.associatedDomain.isNullOrBlank() && it.iconCustomPath.isNullOrBlank() }
            .forEach { summary ->
                scope.launch(Dispatchers.IO) {
                    val outcome = vaultUseCases.downloadFavicon(summary.associatedDomain!!)
                    if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                        // 获取完整实体并更新
                        vaultUseCases.getEntryById(summary.id)?.let { entry ->
                            val updated = entry.copy(iconCustomPath = outcome.filePath)
                            vaultUseCases.updateEntry(updated)
                        }
                    }
                }
            }
    }

    suspend fun updateEntry(entry: VaultEntry) {
        vaultUseCases.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: VaultEntry) {
        entryFileSupport.cleanupIcon(entry.iconCustomPath)
        vaultUseCases.deleteEntry(entry)
    }

    suspend fun saveCustomIcon(context: Context, item: VaultEntry, uri: Uri): VaultEntry? {
        val internalPath = entryFileSupport.saveCustomIcon(context, item, uri) ?: return null
        return item.copy(
            iconName = null,
            iconCustomPath = internalPath
        )
    }
}
