package com.aozijx.passly.features.vault.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.domain.usecase.vault.VaultUseCases
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class EntryManager(
    private val scope: CoroutineScope,
    private val vaultUseCases: VaultUseCases,
    private val iconHelper: EntryIconHelper,
    private val detail: DetailCoordinator,
    private val totp: TotpCoordinator
) {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        Logcat.e("EntryManager", "Operation failed", throwable)
    }
    private val deletingIds = mutableSetOf<Int>()
    private val deletingIdsMutex = Mutex()

    fun addItem(entry: VaultEntry, domain: String? = null, onComplete: () -> Unit = {}) {
        scope.launch(Dispatchers.IO + handler) {
            vaultUseCases.addEntry(entry, domain)
            detail.setAddType(null)
            onComplete()
        }
    }

    fun updateEntry(entry: VaultEntry) {
        scope.launch(Dispatchers.IO + handler) {
            vaultUseCases.updateEntry(entry)
            detail.updateEntry(entry)
            totp.onEntryUpdated(entry)
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        scope.launch(Dispatchers.IO + handler) {
            deleteEntryInternal(entry.id, presetEntry = entry)
        }
    }

    fun deleteEntryById(entryId: Int) {
        scope.launch(Dispatchers.IO + handler) {
            deleteEntryInternal(entryId)
        }
    }

    private suspend fun deleteEntryInternal(entryId: Int, presetEntry: VaultEntry? = null) {
        val acquired = deletingIdsMutex.withLock {
            if (deletingIds.contains(entryId)) false else deletingIds.add(entryId)
        }
        if (!acquired) return

        try {
            val entry = presetEntry ?: vaultUseCases.getEntryById(entryId)
            if (detail.isViewingEntry(entryId)) {
                detail.dismissDetail()
            }
            iconHelper.cleanupIcon(entry?.iconCustomPath)
            entry?.let { vaultUseCases.deleteEntry(it) }
            detail.setItemToDelete(null)
            totp.clearSensitiveState(entryId)
        } finally {
            deletingIdsMutex.withLock { deletingIds.remove(entryId) }
        }
    }

    fun saveCustomIcon(context: Context, item: VaultEntry, uri: Uri, onFailed: () -> Unit = {}) {
        scope.launch(Dispatchers.IO + handler) {
            val internalPath = iconHelper.saveCustomIcon(context, item, uri)
            if (internalPath != null) {
                updateEntry(item.copy(iconName = null, iconCustomPath = internalPath))
            } else {
                onFailed()
            }
        }
    }

    fun downloadMissingIcons(summaries: List<VaultSummary>) {
        scope.launch(Dispatchers.IO + handler) {
            vaultUseCases.downloadMissingFavicons(summaries)
        }
    }
}