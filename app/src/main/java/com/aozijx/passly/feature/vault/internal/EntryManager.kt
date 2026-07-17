package com.aozijx.passly.feature.vault.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.domain.model.entry.VaultEntry
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
    private val totp: TotpCoordinator,
    private val onError: (String) -> Unit = {}
) {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        Logcat.e("EntryManager", "Operation failed", throwable)
        onError("操作失败: ${throwable.message ?: "未知错误"}")
    }
    private val deletingIds = mutableSetOf<String>()
    private val deletingIdsMutex = Mutex()

    fun addItem(entry: VaultEntry, domain: String? = null, onComplete: () -> Unit = {}) {
        scope.launch(Dispatchers.IO + handler) {
            vaultUseCases.addEntry(entry, domain)
                .onSuccess {
                    detail.setAddType(null)
                    onComplete()
                }
                .onFailure { error ->
                    onError(error.message)
                    detail.setAddType(null)
                    onComplete()
                }
        }
    }

    fun updateEntry(entry: VaultEntry) {
        scope.launch(Dispatchers.IO + handler) {
            vaultUseCases.updateEntry(entry)
                .onSuccess {
                    detail.updateEntry(entry)
                    totp.onEntryUpdated(entry)
                }
                .onFailure { error ->
                    onError(error.message)
                }
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        scope.launch(Dispatchers.IO + handler) {
            deleteEntryInternal(entry.id, presetEntry = entry)
        }
    }

    fun deleteEntryById(entryId: String) {
        scope.launch(Dispatchers.IO + handler) {
            deleteEntryInternal(entryId)
        }
    }

    private suspend fun deleteEntryInternal(entryId: String, presetEntry: VaultEntry? = null) {
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
        } catch (e: AppError) {
            onError(e.message)
        } finally {
            deletingIdsMutex.withLock { deletingIds.remove(entryId) }
        }
    }

    fun saveCustomIcon(context: Context, item: VaultEntry, uri: Uri, onFailed: () -> Unit = {}) {
        scope.launch(Dispatchers.IO + handler) {
            try {
                val internalPath = iconHelper.saveCustomIcon(context, item, uri)
                if (internalPath != null) {
                    updateEntry(item.copy(metadata = item.metadata.copy(icon = internalPath)))
                } else {
                    onFailed()
                }
            } catch (e: AppError) {
                onError(e.message)
                onFailed()
            }
        }
    }

    fun downloadMissingIcons(summaries: List<VaultEntry>) {
        scope.launch(Dispatchers.IO + handler) {
            vaultUseCases.downloadMissingFavicons(summaries)
        }
    }
}