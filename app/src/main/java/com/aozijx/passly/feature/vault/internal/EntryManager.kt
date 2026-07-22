package com.aozijx.passly.feature.vault.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.usecase.vault.VaultCommandUseCases
import com.aozijx.passly.domain.usecase.vault.VaultQueryUseCases
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class EntryManager(
    private val scope: CoroutineScope,
    private val vaultCommandUseCases: VaultCommandUseCases,
    private val vaultQueryUseCases: VaultQueryUseCases,
    private val iconHelper: EntryIconHelper,
    private val detail: DetailCoordinator,
    private val totp: TotpCoordinator,
    private val onError: (String) -> Unit = {}
) {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        AppLog.e("EntryManager", "Operation failed", throwable)
        onError("操作失败: ${throwable.message ?: "未知错误"}")
    }
    private val deletingIds = mutableSetOf<String>()
    private val deletingIdsMutex = Mutex()

    fun addItem(entry: VaultEntry, domain: String? = null, onComplete: () -> Unit = {}) {
        scope.launch(Dispatchers.IO + handler) {
            vaultCommandUseCases.addEntry(entry, domain)
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

    /**
     * 接收来自详情页面的完整条目更新，分解为独立的字段命令。
     * 每个详情页面的编辑操作通常只修改一个字段（标题、密码等），
     * 此处通过字段比较确定实际变更的字段并调用对应的命令。
     */
    fun updateEntry(entry: VaultEntry) {
        scope.launch(Dispatchers.IO + handler) {
            val current = vaultQueryUseCases.getById(entry.id) ?: return@launch
            val version = current.metadata.entryVersion

            val result = when {
                current.title != entry.title ->
                    vaultCommandUseCases.updateTitle(entry.id, version, entry.title)

                current.username != entry.username ->
                    vaultCommandUseCases.updateUsername(entry.id, version, entry.username)

                current.metadata.favorite != entry.favorite ->
                    vaultCommandUseCases.toggleFavorite(entry.id, version)

                current.metadata.icon != entry.metadata.icon ->
                    vaultCommandUseCases.setIcon(entry.id, version, entry.metadata.icon)

                current.metadata.website != entry.website ->
                    vaultCommandUseCases.updateWebsite(entry.id, version, entry.website)

                current.credential.password != entry.credential.password ->
                    vaultCommandUseCases.updatePassword(
                        entry.id,
                        version,
                        entry.credential.password ?: ""
                    )

                current.credential.email != entry.credential.email ->
                    vaultCommandUseCases.updateEmail(
                        entry.id,
                        version,
                        entry.credential.email ?: ""
                    )

                current.credential.notes != entry.credential.notes ->
                    vaultCommandUseCases.updateNotes(
                        entry.id,
                        version,
                        entry.credential.notes ?: ""
                    )

                else -> return@launch // nothing changed
            }

            result.onSuccess {
                detail.updateEntry(entry)
                totp.onEntryUpdated(entry.id)
            }.onFailure { error ->
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
            val entry = presetEntry ?: vaultQueryUseCases.getById(entryId)
            if (entry == null) return
            if (detail.isViewingEntry(entryId)) {
                detail.dismissDetail()
            }
            iconHelper.cleanupIcon(entry.iconCustomPath)
            vaultCommandUseCases.moveToTrash(entry.id, entry.metadata.entryVersion)
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
                    vaultCommandUseCases.setIcon(
                        item.id, item.metadata.entryVersion, internalPath
                    ).onSuccess {
                        detail.updateEntry(item.copy(metadata = item.metadata.copy(icon = internalPath)))
                        totp.onEntryUpdated(item.id)
                    }.onFailure { error ->
                        onError(error.message)
                        onFailed()
                    }
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
            vaultCommandUseCases.downloadMissingFavicons(summaries)
        }
    }
}
