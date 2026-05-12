package com.aozijx.passly.features.vault.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.icon.FaviconResult
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.domain.usecase.vault.VaultUseCases
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    fun addItem(entry: VaultEntry, domain: String? = null, onComplete: () -> Unit = {}) {
        scope.launch(Dispatchers.IO + handler) {
            val insertedId = vaultUseCases.insertEntry(entry)
            detail.setAddType(null)

            if (!domain.isNullOrBlank()) {
                val outcome = vaultUseCases.downloadFavicon(domain)
                if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                    val updated = entry.copy(
                        id = insertedId.toInt(), iconName = null, iconCustomPath = outcome.filePath
                    )
                    updateEntry(updated)
                }
            }
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
            if (detail.isViewingEntry(entry.id)) {
                detail.dismissDetail()
                totp.clearSensitiveState(entry.id)
            }
            iconHelper.cleanupIcon(entry.iconCustomPath)
            vaultUseCases.deleteEntry(entry)
            detail.setItemToDelete(null)
            totp.clearSensitiveState(entry.id)
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
        summaries.filter { !it.associatedDomain.isNullOrBlank() && it.iconCustomPath.isNullOrBlank() }
            .forEach { summary ->
                scope.launch(Dispatchers.IO + handler) {
                    val outcome = vaultUseCases.downloadFavicon(summary.associatedDomain!!)
                    if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                        vaultUseCases.getEntryById(summary.id)?.let { entry ->
                            updateEntry(entry.copy(iconCustomPath = outcome.filePath))
                        }
                    }
                }
            }
    }
}