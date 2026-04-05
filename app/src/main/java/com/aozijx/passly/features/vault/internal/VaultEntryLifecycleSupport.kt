package com.aozijx.passly.features.vault.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.domain.model.FaviconResult
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.usecase.vault.VaultUseCases

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
