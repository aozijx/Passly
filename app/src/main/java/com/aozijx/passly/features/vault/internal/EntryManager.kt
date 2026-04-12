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

internal class EntryManager(
    private val vaultUseCases: VaultUseCases,
    private val iconHelper: EntryIconHelper
) {
    suspend fun addEntry(entry: VaultEntry): Long = vaultUseCases.insertEntry(entry)

    suspend fun addEntryWithFavicon(entry: VaultEntry, domain: String) {
        val insertedId = addEntry(entry)
        if (domain.isBlank()) return
        val outcome = vaultUseCases.downloadFavicon(domain)
        if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
            vaultUseCases.updateEntry(
                entry.copy(id = insertedId.toInt(), iconName = null, iconCustomPath = outcome.filePath)
            )
        }
    }

    fun downloadMissingIcons(summaries: List<VaultSummary>, scope: CoroutineScope) {
        summaries
            .filter { !it.associatedDomain.isNullOrBlank() && it.iconCustomPath.isNullOrBlank() }
            .forEach { summary ->
                scope.launch(Dispatchers.IO) {
                    val outcome = vaultUseCases.downloadFavicon(summary.associatedDomain!!)
                    if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                        vaultUseCases.getEntryById(summary.id)?.let { entry ->
                            vaultUseCases.updateEntry(entry.copy(iconCustomPath = outcome.filePath))
                        }
                    }
                }
            }
    }

    suspend fun updateEntry(entry: VaultEntry) = vaultUseCases.updateEntry(entry)

    suspend fun deleteEntry(entry: VaultEntry) {
        iconHelper.cleanupIcon(entry.iconCustomPath)
        vaultUseCases.deleteEntry(entry)
    }

    suspend fun saveCustomIcon(context: Context, item: VaultEntry, uri: Uri): VaultEntry? {
        val internalPath = iconHelper.saveCustomIcon(context, item, uri) ?: return null
        return item.copy(iconName = null, iconCustomPath = internalPath)
    }
}
